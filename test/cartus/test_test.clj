(ns cartus.test-test
  (:require
   [clojure.test :refer :all]

   [cartus.test-support.definitions :as defs]

   [cartus.core :as cartus]
   [cartus.test :as cartus-test]))

(deftest logs-to-test-logger-at-level-with-type-context-and-correct-meta
  (doseq [{:keys [level-keyword without-opts]} defs/level-defs]
    (let [{:keys [fn meta]} without-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}]
      (fn logger type context)

      (is (= [{:level   level-keyword
               :type    type
               :context context
               :meta    (assoc meta
                          :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/records logger))))))

(deftest logs-to-test-logger-at-level-using-specified-message-when-provided
  (doseq [{:keys [level-keyword with-opts]} defs/level-defs]
    (let [{:keys [fn meta]} with-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}
          message "Some event just happened."]
      (fn logger type context {:message message})

      (is (= [{:level   level-keyword
               :type    type
               :context context
               :message message
               :meta    (assoc meta
                          :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/records logger))))))

(deftest logs-to-test-logger-at-level-using-specified-exception-when-provided
  (doseq [{:keys [level-keyword with-opts]} defs/level-defs]
    (let [{:keys [fn meta]} with-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}
          exception (ex-info "Something went wrong..." {:some "data"})]
      (fn logger type context {:exception exception})

      (is (= [{:level     level-keyword
               :type      type
               :context   context
               :exception exception
               :meta      (assoc meta
                            :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/records logger))))))
