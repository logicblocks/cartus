(ns cartus.test
  "A [[cartus.core/Logger]] implementation and utilities for use in tests."
  (:require
   [clojure.test :as test]

   [matcher-combinators.core :as mc-core]
   [matcher-combinators.clj-test :as mc-test]

   [cartus.core :as cartus]
   [cartus.test.matchers :as cartus-matchers]))

(defrecord TestLogger
  [events]
  cartus/Logger
  (log [_ level type context opts]
    (swap! events conj
      (merge opts
        {:level   level
         :type    type
         :context context}))))

(defn logger
  "Constructs a test logger storing all logged events in an atom.

  Events are stored as maps including all provided attributes, in the order in
  which they occur, accounting for concurrency.

  Events can be retrieved using [[events]]."
  []
  (map->TestLogger {:events (atom [])}))

(defn events
  "Retrieves events logged to the provided `test-logger`."
  [test-logger]
  @(:events test-logger))

#{:in-order :only :fuzzy-contents}
#{:in-order :only :strict-contents}
#{:in-any-order :only :fuzzy-contents}                      ; (mc-matchers/in-any-order delegate)
#{:in-any-order :only :strict-contents}
#{:in-order :at-least :without-gaps :fuzzy-contents}
#{:in-order :at-least :without-gaps :strict-contents}
#{:in-order :at-least :with-gaps :fuzzy-contents}           ; <= default
#{:in-order :at-least :with-gaps :strict-contents}
#{:in-any-order :at-least :without-gaps :fuzzy-contents}
#{:in-any-order :at-least :without-gaps :strict-contents}
#{:in-any-order :at-least :with-gaps :fuzzy-contents}       ; (mc-matchers/match-with [vector? mc-matchers/embeds] delegate)
#{:in-any-order :at-least :with-gaps :strict-contents}

(def call-expectation
  (symbol
    (str
      "logged? to be called with a test logger, an optional set "
      "of modifiers and at least one log event spec")))

(defmethod test/assert-expr 'logged? [msg form]
  `(let [args# (list ~@(rest form))
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
          :expected call-expectation
          :actual   (symbol
                      (str "only " arg-count# " argument was provided: "
                        '~form))})

       (empty? resolved-log-specs#)
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation
          :actual   (symbol
                      (str "no log specs were provided: " '~form))})

       (not (or (map? modifiers#) (set? modifiers#)))
       (test/do-report
         {:type     :fail
          :message  ~msg
          :expected call-expectation
          :actual   (symbol
                      (str "non-set modifiers provided: " '~form))})

       (not (instance? TestLogger logger#))
       (test/do-report
         {:type :fail
          :message ~msg
          :expected call-expectation
          :actual (symbol
                    (str "instance other than test logger provided: "
                      '~form))})

       (not (mc-core/matcher? resolved-log-specs#))
       (test/do-report
         {:type :fail
          :message ~msg
          :expected call-expectation
          :actual (symbol
                    (str "non-matcher log specs provided: " '~form))})

       :else
       (let [matcher# (cartus-matchers/subsequences resolved-log-specs#)
             result# (mc-core/match matcher# (cartus-test/events logger#))
             match?# (mc-core/indicates-match? result#)]
         (test/do-report
           (if match?#
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
                          result#)}))
         match?#))))
