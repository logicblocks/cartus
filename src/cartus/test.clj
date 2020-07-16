(ns cartus.test
  (:require
   [cartus.core :as cartus]))

(defrecord TestLogger
  [records]
  cartus/Logger
  (log [_ level type context opts]
    (swap! records conj
      (merge opts
        {:level   level
         :type    type
         :context context}))))

(defn logger []
  (map->TestLogger {:records (atom [])}))

(defn records [test-logger]
  @(:records test-logger))
