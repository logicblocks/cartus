(ns cartus.cambium.json-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [cartus.cambium :as cartus-cambium]
            [cartus.cambium.json :refer [add-java-time-encoders!]]

            [cartus.test-support.definitions :as defs]
            [cartus.test-support.logback :as logback]

            [cheshire.core :as json])
  (:import
   (java.time
     Instant Period Duration)
   (java.util
     UUID)))

(add-java-time-encoders!)

(defn log-lines [output-stream]
  (as-> output-stream lines
    (str lines)
    (string/split-lines lines)
    (map #(json/parse-string % keyword) lines)
    (map #(dissoc % :timestamp :thread) lines)))

(deftest logs-with-default-string-encoding-of-shallow-values
  (doseq [{:keys [without-opts]} defs/level-defs]
    (let [{:keys [log-fn]} without-opts
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          a-uuid-str "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
          unencodable
          (proxy [Object] []
            (toString []
              (throw (Exception. "You shouldn't have called me!"))))
          context {:a-uuid       (UUID/fromString a-uuid-str)
                   :an-instant   (Instant/ofEpochMilli 0)
                   :a-duration (Duration/ofDays 4)
                   :a-period (Period/ofWeeks 2)
                   :unencodeable unencodable}]
      (cartus-cambium/initialise)

      (log-fn logger type context)

      (is (= [{:a-uuid       "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
               :an-instant   "1970-01-01T00:00:00Z"
               :a-duration "PT96H"
               :a-period     "P14D"
               :unencodeable "Unable to encode MDC value as JSON"}]
            (->> (log-lines log-output-stream)
              (map #(select-keys % (keys context)))))))))

(deftest logs-with-default-string-encoding-of-nested-values
  (doseq [{:keys [without-opts]} defs/level-defs]
    (let [{:keys [log-fn]} without-opts
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          a-uuid-str "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
          context {:a-nested-map
                   {:a-uuid     (UUID/fromString a-uuid-str)
                    :a-duration (Duration/ofDays 1)
                    :a-period (Period/ofMonths 3)
                    :an-instant (Instant/ofEpochMilli 0)}}]
      (cartus-cambium/initialise)

      (log-fn logger type context)

      (is (= [{:a-nested-map
               {:a-uuid     "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
                :a-duration "PT24H"
                :a-period   "P3M"
                :an-instant "1970-01-01T00:00:00Z"}}]
            (->> (log-lines log-output-stream)
              (map #(select-keys % (keys context)))))))))
