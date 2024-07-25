(ns cartus.test
  "A [[cartus.core/Logger]] implementation and utilities for use in tests."
  (:require
   [clojure.test :as test]
   [clojure.set :as sets]

   [matcher-combinators.core :as mc-core]
   [matcher-combinators.matchers :as mc-matchers]
   [matcher-combinators.clj-test :as mc-test]

   [cartus.core :as cartus]
   [cartus.test.matchers :as cartus-matchers])
  (:import (cartus.core CompositeLogger TransformerLogger)))

(defprotocol TestEventStorage
  (-get-events [logger])
  (-clear-events! [logger]))

(deftype TestLogger
  [events]
  cartus/Logger
  (log [_ level type context opts]
    (swap! events conj
      (merge opts
        {:level   level
         :type    type
         :context context})))

  TestEventStorage
  (-get-events [_]
    @events)
  (-clear-events! [_]
    (reset! events [])))

(extend-protocol TestEventStorage
  TransformerLogger
  (-get-events [tx-logger]
    (-get-events (:delegate tx-logger)))
  (-clear-events! [tx-logger]
    (-clear-events! (:delegate tx-logger)))

  CompositeLogger
  (-get-events [composite-logger]
    (->> composite-logger
      :loggers
      (filter #(satisfies? TestEventStorage %))
      (mapcat -get-events)))
  (-clear-events! [composite-logger]
    (->> composite-logger
      :loggers
      (filter #(satisfies? TestEventStorage %))
      (map -clear-events!)
      doall)))

(defn logger
  "Constructs a test logger storing all logged events in an atom.

  Events are stored as maps including all provided attributes, in the order in
  which they occur, accounting for concurrency.

  Events can be retrieved using [[events]]."
  []
  (TestLogger. (atom [])))

(defn clear-events!
  "Clears events logged to the provided `logger`."
  [logger]
  (-clear-events! logger))

(defn events
  "Retrieves events logged to the provided `logger`."
  [logger]
  (-get-events logger))

(defn create-outcome [logger modifiers log-specs]
  (let [overrides {}
        overrides (if (:strict-contents modifiers)
                    (merge overrides {map? mc-matchers/equals})
                    overrides)

        matcher (cond
                  (sets/subset?
                    #{:only :in-any-order} modifiers)
                  (mc-matchers/in-any-order log-specs)

                  (:only modifiers)
                  (mc-matchers/equals log-specs)

                  (:in-any-order modifiers)
                  (mc-matchers/embeds log-specs)

                  :else
                  (cartus-matchers/subsequences log-specs))
        matcher (if (not-empty overrides)
                  (mc-matchers/match-with overrides matcher)
                  matcher)

        result (mc-core/match matcher (events logger))
        match? (mc-core/indicates-match? result)]
    {:result result
     :match? match?}))

(defn was-logged?
  "Returns true if the given log-specs were logged.

  Takes a logger, a set of modifiers and a variable number of log specs:

  - `logger` must be a [[cartus.test/logger]]
  - `modifiers` must be a set, optionally containing one of each of:
    - `#{:in-order :in-any-order}` to specify ordering constraints, defaults
      to `:in-order`
    - `#{:only :at-least}` to specify whether the provided logs must exactly
      match the log events logged to the logger or whether they represent
      a subset, defaults to `:at-least`
    - `#{:fuzzy-contents :strict-contents}` to specify whether log events
      should be matched fuzzily, i.e., surplus keys can be present in the
      log event map, or strictly, i.e., the keys and values must match
      exactly, defaults to `:fuzzy-contents`
  - each log spec is a partial or full map of the log event as returned
    by [[events]].
  "
  [logger modifiers & log-specs]
  (let [{:keys [match?]} (create-outcome logger modifiers log-specs)]
    match?))

(declare
  ^{:doc
    "Asserts that the logger received log events matching the provided log
    specs.

    Takes either a logger and a variable number of log specs or a logger,
    a set of modifiers and a variable number of log specs:

      - `logger` must be a [[cartus.test/logger]]
      - `modifiers` must be a set, optionally containing one of each of:
        - `#{:in-order :in-any-order}` to specify ordering constraints, defaults
          to `:in-order`
        - `#{:only :at-least}` to specify whether the provided logs must exactly
          match the log events logged to the logger or whether they represent
          a subset, defaults to `:at-least`
        - `#{:fuzzy-contents :strict-contents}` to specify whether log events
          should be matched fuzzily, i.e., surplus keys can be present in the
          log event map, or strictly, i.e., the keys and values must match
          exactly, defaults to `:fuzzy-contents`
      - each log spec is a partial or full map of the log event as returned
        by [[events]].

    Internally, `logged?` uses the `matcher-combinators` library meaning more
    complex log specs can be provided, including using predicates to match parts
    of the log events. See the
    [Getting Started](https://logicblocks.github.io/cartus/getting-started.html)
    guide for more details.

    Examples:

    ```
    (is (logged? logger
      {:level   :debug
       :type    :service.database/connection-pool.started
       :context {:max-connections 10}}))
    ```

    ```
    (is (logged? logger #{:in-any-order :strict-contents}
      {:level   :info
       :type    :service.database/connection-pool.online.starting
       :context {:max-connections 10}
       :meta    {:ns     (find-ns 'service.database)
                 :line   1
                 :column 1}}
       {:level   :info
       :type    :service.database/connection-pool.batch.starting
       :context {:max-connections 3}
       :meta    {:ns     (find-ns 'service.database)
                 :line   2
                 :column 1}}))
    ```"
    :arglists
    '([logger & log-specs]
      [logger modifiers & log-specs])}
  logged?)

(defmethod test/assert-expr 'logged? [msg form]
  `(let [valid-modifiers?#
         (fn [modifiers#]
           (not
             (or
               (sets/subset? #{:fuzzy-contents :strict-contents} modifiers#)
               (sets/subset? #{:only :at-least} modifiers#)
               (sets/subset? #{:in-order :in-any-order} modifiers#))))

         call-expectation#
         (symbol
           (str
             "logged? to be called with a test logger, an optional set "
             "of modifiers and at least one log event spec"))

         args# (list ~@(rest form))
         arg-count# (count args#)

         [logger# modifiers# & log-specs#] args#

         resolved-log-specs#
         (if (map? modifiers#)
           (vec (cons modifiers# log-specs#))
           log-specs#)

         resolved-modifiers#
         (if (set? modifiers#)
           modifiers#
           #{})]
     (cond
       (= arg-count# 1)
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "only " arg-count# " argument was provided: "
                        '~form))})

       (empty? resolved-log-specs#)
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "no log specs were provided: " '~form))})

       (not (or (map? modifiers#) (set? modifiers#)))
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "non-set modifiers provided: " '~form))})

       (not (instance? TestLogger logger#))
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "instance other than test logger provided: "
                        '~form))})

       (not (mc-core/matcher? resolved-log-specs#))
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "non-matcher log specs provided: " '~form))})

       (not (valid-modifiers?# resolved-modifiers#))
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation#
          :actual   (symbol
                      (str "invalid combination of modifiers provided: "
                        '~form))})

       :else
       (let [result# (create-outcome
                       logger# resolved-modifiers# resolved-log-specs#)]
         (test/do-report
           (if (:match? result#)
             {:type     :pass
              :message  ~msg
              :expected '~form
              :actual   `('logged? ~logger# ~modifiers# ~@log-specs#)}
             {:type     :fail
              :message  ~msg
              :expected '~form
              :actual   (mc-test/tagged-for-pretty-printing
                          (list '~'not
                            `('logged? ~logger# ~modifiers# ~@log-specs#))
                          (:result result#))}))
         (:match? result#)))))
