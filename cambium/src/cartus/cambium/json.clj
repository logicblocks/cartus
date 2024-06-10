(ns cartus.cambium.json
  (:require [cheshire.generate :as cg])
  (:import [com.fasterxml.jackson.core JsonGenerator]
           [java.time Instant]))

(defn add-java-time-encoders! []
  (cg/add-encoder Instant
    (fn [value jsonGenerator]
      (.writeString
        ^JsonGenerator jsonGenerator
        ^String (str value)))))
