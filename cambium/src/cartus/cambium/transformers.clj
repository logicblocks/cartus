(ns cartus.cambium.transformers
  (:import (java.util LinkedHashMap List Map)))

(defn- key-position [^List key-ordering k]
  (let [pos (.indexOf key-ordering (name k))]
    (if (neg? pos)
      (Integer/MAX_VALUE)
      pos)))

(def default-ordering
  ["level" "type" "message"])

(defn key-order-transformer
  ([]
   (key-order-transformer default-ordering))
  ([key-ordering]
   (fn [^Map m]
     (let [entries (vec m)
           sorted-entries (sort-by (fn [[k _]] (key-position key-ordering k)) entries)
           ordered-map (LinkedHashMap.)]
       (doseq [[k v] sorted-entries]
         (.put ordered-map k v))
       ordered-map))))
