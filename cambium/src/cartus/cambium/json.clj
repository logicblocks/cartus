(ns cartus.cambium.json
  (:require [cheshire.generate :as cg])
  (:import [com.fasterxml.jackson.core JsonGenerator]
           [java.time Instant Duration Period]))

(defn add-java-time-encoders! []
  (cg/add-encoder Instant
    (fn [value jsonGenerator]
      (.writeString
        ^JsonGenerator jsonGenerator
        ^String (str value))))
  (cg/add-encoder Duration
    (fn [value jsonGenerator]
      (.writeString
        ^JsonGenerator jsonGenerator
        ^String (str value))))
  (cg/add-encoder Period
    (fn [value jsonGenerator]
      (.writeString
        ^JsonGenerator jsonGenerator
        ^String (str value)))))
