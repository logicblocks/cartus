(ns cartus.cambium-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :as string]

    [cheshire.core :as json]

    [cartus.test-support.definitions :as defs]
    [cartus.test-support.logback :as logback]

    [cartus.cambium :as cartus-cambium])
  (:import
    (java.time
      Instant)
    (java.util
      UUID)))

(defn log-lines [output-stream]
  (as-> output-stream lines
    (str lines)
    (string/split-lines lines)
    (map #(json/parse-string % keyword) lines)
    (map #(dissoc % :timestamp :thread) lines)))

(deftest logs-to-cambium-logger-at-level-with-type-context-and-correct-meta
  (doseq [{:keys [slf4j-level-name without-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} without-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}]
      (cartus-cambium/initialise)

      (log-fn logger type context)

      (is (= [{:level   (string/upper-case slf4j-level-name)
               :some    "context"
               :type    "cartus.cambium-test/some.event"
               :ns      "cartus.test-support.definitions"
               :line    line
               :column  column
               :logger  "cartus.test-support.definitions"
               :message "cartus.cambium-test/some.event"
               :context "default"}]
            (log-lines log-output-stream))))))

(deftest logs-to-cambium-logger-at-level-using-specified-message-when-provided
  (doseq [{:keys [slf4j-level-name with-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} with-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}
          message "Some event just happened."]
      (cartus-cambium/initialise)

      (log-fn logger type context
        {:message message})

      (is (= [{:level   (string/upper-case slf4j-level-name)
               :some    "context"
               :type    "cartus.cambium-test/some.event"
               :ns      "cartus.test-support.definitions"
               :line    line
               :column  column
               :logger  "cartus.test-support.definitions"
               :message message
               :context "default"}]
            (log-lines log-output-stream))))))

(deftest logs-to-cambium-logger-at-level-using-specified-exception-when-provided
  (doseq [{:keys [slf4j-level-name with-opts]} defs/level-defs]
    (let [{:keys [log-fn meta]} with-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}
          exception (ex-info "Something went wrong..." {:some "data"})]
      (cartus-cambium/initialise)

      (log-fn logger type context {:exception exception})

      (is (= [{:level     (string/upper-case slf4j-level-name)
               :some      "context"
               :type      "cartus.cambium-test/some.event"
               :ns        "cartus.test-support.definitions"
               :line      line
               :column    column
               :logger    "cartus.test-support.definitions"
               :message   "cartus.cambium-test/some.event"
               :exception (logback/log-formatted-exception exception)
               :context   "default"}]
            (log-lines log-output-stream))))))

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
                   :unencodeable unencodable}]
      (cartus-cambium/initialise)

      (log-fn logger type context)

      (is (= [{:a-uuid       "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
               :an-instant   "1970-01-01T00:00:00Z"
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
                   {:a-uuid       (UUID/fromString a-uuid-str)
                    :an-instant   (Instant/ofEpochMilli 0)}}]
      (cartus-cambium/initialise)

      (log-fn logger type context)

      (is (= [{:a-nested-map
               {:a-uuid       "ae355a31-a07b-44f0-9d3f-6dc2284da37f"
                :an-instant   "1970-01-01T00:00:00Z"}}]
            (->> (log-lines log-output-stream)
              (map #(select-keys % (keys context)))))))))
