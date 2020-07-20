(ns cartus.test
  "A [[cartus.core/Logger]] implementation and utilities for use in tests."
  (:require
   [cartus.core :as cartus]))

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
