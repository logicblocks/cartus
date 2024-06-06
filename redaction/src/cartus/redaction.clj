(ns cartus.redaction
  (:require [clojure.walk :as walk]
            [cartus.core :as log]))

(defn redact
  [m {:keys [redact-value-fn]}]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (->> x
          (map (fn [[k v]]
                 (if-let [redaction (redact-value-fn k v)]
                   [k redaction]
                   [k v])))
          (into {}))
        x))
    m))

(def default-redaction-options
  {:redact-value-fn
   (fn [k _v]
     (when (#{:authorization :password :token
              :secret :secret-key :secret-token} (keyword k))
       "[REDACTED]"))})

(defn with-redaction
  "Applys a transformation to a logger to redact sensitive values from
   log context.

   arity-1 version expects a cartus logger to transform
   arity-2 version expects a cartus logger plus override redact-options

   redact-options:

   `{:redact-value-fn redact-value-fn}`

   Where `redact-value-fn` takes two arguments, the key and value at
   a specific point in a data structure during traversal.  If the function
   returns a value, that value is used as a substitute for the existing value.
  "
  ([logger]
   (with-redaction logger default-redaction-options))
  ([logger redact-options]
   (log/with-transformation
     logger
     (map #(update % :context redact redact-options)))))
