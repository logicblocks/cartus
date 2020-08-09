(ns cartus.test-test
  (:require
   [clojure.test :refer [deftest is]]

   [cartus.core :as cartus-core]
   [cartus.test :as cartus-test]

   [cartus.test-support.definitions :as defs]
   [cartus.test-support.reports :as reports]))

(deftest logs-to-test-logger-at-level-with-type-context-and-correct-meta
  (doseq [{:keys [level-keyword without-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} without-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}]
      (log-fn logger type context)

      (is (= [{:level   level-keyword
               :type    type
               :context context
               :meta    (assoc meta
                          :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/events logger))))))

(deftest logs-to-test-logger-at-level-using-specified-message-when-provided
  (doseq [{:keys [level-keyword with-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} with-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}
          message "Some event just happened."]
      (log-fn logger type context {:message message})

      (is (= [{:level   level-keyword
               :type    type
               :context context
               :message message
               :meta    (assoc meta
                          :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/events logger))))))

(deftest logs-to-test-logger-at-level-using-specified-exception-when-provided
  (doseq [{:keys [level-keyword with-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} with-opts
          logger (cartus-test/logger)
          type ::some.event
          context {:some "context"}
          exception (ex-info "Something went wrong..." {:some "data"})]
      (log-fn logger type context {:exception exception})

      (is (= [{:level     level-keyword
               :type      type
               :context   context
               :exception exception
               :meta      (assoc meta
                            :ns (find-ns 'cartus.test-support.definitions))}]
            (cartus-test/events logger))))))

(deftest logged?-matches-single-log-event-exactly
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}

        _ ^{:line 1 :column 1} (cartus-core/info logger type context)

        log-event {:level   :info
                   :type    type
                   :context context
                   :meta    {:ns     (find-ns 'cartus.test-test)
                             :line   1
                             :column 1}}

        report (reports/report-on (logged? logger log-event))]
    (is (= :pass (:type report)))))

(deftest logged?-matches-multiple-log-event-exactly
  (let [logger (cartus-test/logger)
        type-1 ::some.event.1
        type-2 ::some.event.2
        context {:some "context"}

        _ ^{:line 1 :column 1} (cartus-core/info logger type-1 context)
        _ ^{:line 2 :column 1} (cartus-core/info logger type-2 context)

        log-event-1 {:level   :info
                     :type    type-1
                     :context context
                     :meta    {:ns     (find-ns 'cartus.test-test)
                               :line   1
                               :column 1}}
        log-event-2 {:level   :info
                     :type    type-2
                     :context context
                     :meta    {:ns     (find-ns 'cartus.test-test)
                               :line   2
                               :column 1}}

        report (reports/report-on (logged? logger log-event-1 log-event-2))]
    (is (= :pass (:type report)))))

(deftest logged?-uses-fuzzy-matching-on-log-event-by-default
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}

        _ (cartus-core/info logger type context)

        log-event {:type    type
                   :context context}

        report (reports/report-on (logged? logger log-event))]
    (is (= :pass (:type report)))))

(deftest logged?-fails-when-no-log-events-available
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}

        log-event {:level   :info
                   :type    type
                   :context context}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(reports/missing log-event)]
          (:matcher-combinators.result/value match-result)))))

(deftest logged?-fails-when-log-event-has-incorrect-level
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}

        _ (cartus-core/info logger type context)

        log-event {:level   :debug
                   :type    type
                   :context context}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:level (reports/mismatch
                        {:expected :debug
                         :actual   :info})})]
          (map #(dissoc % :meta)
            (:matcher-combinators.result/value match-result))))))

(deftest logged?-fails-when-log-event-has-incorrect-type
  (let [logger (cartus-test/logger)
        logged-type ::some.event
        expected-type ::other.event
        context {:some "context"}

        _ (cartus-core/info logger logged-type context)

        log-event {:level   :info
                   :type    expected-type
                   :context context}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:type (reports/mismatch
                       {:expected expected-type
                        :actual   logged-type})})]
          (map #(dissoc % :meta)
            (:matcher-combinators.result/value match-result))))))

(deftest logged?-fails-when-log-event-has-incorrect-context
  (let [logger (cartus-test/logger)
        type ::some.event
        logged-context {:some {:logged "context"}}
        expected-context {:some {:expected "context"}}

        _ (cartus-core/info logger type logged-context)

        log-event {:level   :info
                   :type    type
                   :context expected-context}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:context
               {:some
                {:expected (reports/missing "context")
                 :logged   "context"}}})]
          (map #(dissoc % :meta)
            (:matcher-combinators.result/value match-result))))))

(deftest logged?-fails-when-log-event-has-incorrect-meta
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}

        _ ^{:line 2 :column 2} (cartus-core/info logger type context)

        log-event {:level   :info
                   :type    type
                   :context context
                   :meta    {:ns     (find-ns 'cartus.test-test)
                             :line   2
                             :column 1}}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:meta
               {:ns     (find-ns 'cartus.test-test)
                :line   2
                :column (reports/mismatch {:expected 1 :actual 2})}})]
          (:matcher-combinators.result/value match-result)))))

(deftest logged?-fails-when-log-event-has-incorrect-message
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}
        logged-message "Some logged message"
        expected-message "Some expected message"

        _ (cartus-core/info logger type context
            {:message logged-message})

        log-event {:level   :info
                   :type    type
                   :context context
                   :message expected-message}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:message (reports/mismatch
                          {:expected expected-message
                           :actual   logged-message})})]
          (map #(dissoc % :meta)
            (:matcher-combinators.result/value match-result))))))

(deftest logged?-fails-when-log-event-has-incorrect-exception
  (let [logger (cartus-test/logger)
        type ::some.event
        context {:some "context"}
        logged-exception (ex-info "Oops" {:value 1})
        expected-exception (ex-info "Golly" {:value 2})

        _ (cartus-core/info logger type context
            {:exception logged-exception})

        log-event {:level     :info
                   :type      type
                   :context   context
                   :exception expected-exception}

        report (reports/report-on (logged? logger log-event))

        match-result (get-in report [:actual :match-result])]
    (is (= :fail (:type report)))
    (is (= :mismatch (:matcher-combinators.result/type match-result)))
    (is (= [(merge log-event
              {:exception
               (reports/mismatch
                 {:expected expected-exception
                  :actual   logged-exception})})]
          (map #(dissoc % :meta)
            (:matcher-combinators.result/value match-result))))))

(deftest logged?-fails-when-passed-only-a-logger
  (let [logger (cartus-test/logger)

        report (reports/report-on (logged? logger))]
    (is (= :fail (:type report)))
    (is (= (str "logged? to be called with a test logger, an optional set of "
             "modifiers and at least one log event spec")
          (name (:expected report))))
    (is (= "only 1 argument was provided: (logged? logger)"
          (name (:actual report))))))

(deftest logged?-fails-when-passed-only-a-logger-and-modifiers
  (let [logger (cartus-test/logger)
        modifiers #{:in-any-order :only}

        report (reports/report-on (logged? logger modifiers))]
    (is (= :fail (:type report)))
    (is (= (str "logged? to be called with a test logger, an optional set of "
             "modifiers and at least one log event spec")
          (name (:expected report))))
    (is (= "no log specs were provided: (logged? logger modifiers)"
          (name (:actual report))))))

(deftest logged?-fails-when-passed-non-set-modifiers
  (let [logger (cartus-test/logger)
        modifiers [:all :wrong]
        log-spec {:level :info}

        report (reports/report-on (logged? logger modifiers log-spec))]
    (is (= :fail (:type report)))
    (is (= (str "logged? to be called with a test logger, an optional set of "
             "modifiers and at least one log event spec")
          (name (:expected report))))
    (is (= "non-set modifiers provided: (logged? logger modifiers log-spec)"
          (name (:actual report))))))

(deftest logged?-fails-when-passed-non-logger
  (let [logger {}
        log-spec {:level :info}

        report (reports/report-on (logged? logger log-spec))]
    (is (= :fail (:type report)))
    (is (= (str "logged? to be called with a test logger, an optional set of "
             "modifiers and at least one log event spec")
          (name (:expected report))))
    (is (= (str "instance other than test logger provided: "
             "(logged? logger log-spec)")
          (name (:actual report))))))

(deftest logged?-allows-surplus-log-events-by-default
  (let [logger (cartus-test/logger)
        type-1 ::some.event.1
        type-2 ::some.event.2
        type-3 ::some.event.3
        context {:some "context"}

        _ (cartus-core/info logger type-1 context)
        _ (cartus-core/info logger type-2 context)
        _ (cartus-core/info logger type-3 context)

        log-event {:level   :info
                   :type    type-2
                   :context context}

        report (reports/report-on (logged? logger log-event))]
    (is (= :pass (:type report)))))
