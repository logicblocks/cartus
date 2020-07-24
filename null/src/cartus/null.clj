(ns cartus.null
  "A null [[cartus.core/Logger]] implementation that ignores all logged events."
  (:require
   [cartus.core :as cartus]))

(defrecord NullLogger
  []
  cartus/Logger
  (log [_ _ _ _ _]))

(defn logger
  "Constructs a null logger which ignores all events."
  []
  (->NullLogger))
