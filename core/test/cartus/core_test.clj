(ns cartus.core-test
  (:require
   [clojure.test :refer :all]

   [spy.core :as spy]

   [cartus.test-support.definitions :as defs]

   [cartus.core :as cartus]))

(defrecord SpyingLogger
  [spy]
  cartus/Logger
  (log [_ level type context opts]
    (spy {:level   level
          :type    type
          :context context
          :opts    opts})))

(defn spying-logger []
  (map->SpyingLogger {:spy (spy/stub nil)}))

(deftest adds-context-to-test-logger-with-log-time-context-priority
  (doseq [{:keys [without-opts]} defs/level-defs]
    (let [{:keys [log-fn]} without-opts
          call-context {:first  10
                        :second 20}
          logger-context {:first 1
                          :third 30}

          spying-logger (spying-logger)
          amended-logger (cartus/with-context
                           spying-logger logger-context)

          type ::some.event]
      (log-fn amended-logger type call-context)

      (is (= {:first 10 :second 20 :third 30}
            (-> (spy/calls (:spy spying-logger)) ffirst :context))))))

(deftest retains-logged-events-with-provided-levels
  (let [spying-logger (spying-logger)
        amended-logger (cartus/with-levels-retained
                         spying-logger #{:info :warn :error})

        context {:some "context"}]
    (cartus/debug amended-logger ::debug.event context)
    (cartus/warn amended-logger ::warn.event context)
    (cartus/info amended-logger ::info.event context)

    (is (= [{:level   :warn
             :type    ::warn.event
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   48
                              :column 5}}}
            {:level   :info
             :type    ::info.event
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   49
                              :column 5}}}]
          (map first (spy/calls (:spy spying-logger)))))))

(deftest ignores-logged-events-with-provided-levels
  (let [spying-logger (spying-logger)
        amended-logger (cartus/with-levels-ignored
                         spying-logger #{:trace :debug :info})

        context {:some "context"}]
    (cartus/debug amended-logger ::debug.event context)
    (cartus/warn amended-logger ::warn.event context)
    (cartus/info amended-logger ::info.event context)

    (is (= [{:level   :warn
             :type    ::warn.event
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   72
                              :column 5}}}]
          (map first (spy/calls (:spy spying-logger)))))))

(deftest retains-logged-events-with-provided-types
  (let [spying-logger (spying-logger)
        amended-logger (cartus/with-types-retained
                         spying-logger #{::type-1 ::type-2})

        context {:some "context"}]
    (cartus/info amended-logger ::type-1 context)
    (cartus/info amended-logger ::type-2 context)
    (cartus/info amended-logger ::type-3 context)

    (is (= [{:level   :info
             :type    ::type-1
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   89
                              :column 5}}}
            {:level   :info
             :type    ::type-2
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   90
                              :column 5}}}]
          (map first (spy/calls (:spy spying-logger)))))))

(deftest ignores-logged-events-with-provided-types
  (let [spying-logger (spying-logger)
        amended-logger (cartus/with-types-ignored
                         spying-logger #{::type-2 ::type-3})

        context {:some "context"}]
    (cartus/info amended-logger ::type-1 context)
    (cartus/info amended-logger ::type-2 context)
    (cartus/info amended-logger ::type-3 context)

    (is (= [{:level   :info
             :type    ::type-1
             :context context
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   113
                              :column 5}}}]
          (map first (spy/calls (:spy spying-logger)))))))

(deftest applies-transformation-to-logged-events
  (let [logger (spying-logger)
        type-1 ::event.type.1
        type-2 ::event.type.2
        context {:some "context"}

        xform-1
        (filter
          (fn [event]
            (not= (:type event) ::event.type.2)))
        xform-2
        (map
          (fn [event]
            (update-in event [:context] assoc :other "context")))

        transformed-logger
        (cartus/with-transformation logger
          (comp xform-1 xform-2))]
    (cartus/info transformed-logger type-1 context)
    (cartus/info transformed-logger type-2 context)

    (is (= [{:level   :info
             :type    type-1
             :context (assoc context :other "context")
             :opts    {:meta {:ns     (find-ns 'cartus.core-test)
                              :line   143
                              :column 5}}}]
          (map first (spy/calls (:spy logger)))))))
