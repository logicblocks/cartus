(ns cartus.cambium-test
  (:require
   [clojure.test :refer :all]
   [clojure.string :as string]

   [cheshire.core :as json]

   [cartus.test-support.definitions :as defs]
   [cartus.test-support.logback :as logback]

   [cartus.cambium :as cartus-cambium]))

(defn log-lines [output-stream]
  (as-> output-stream lines
    (str lines)
    (string/split-lines lines)
    (map #(json/parse-string % keyword) lines)
    (map #(dissoc % :timestamp :thread) lines)))

(deftest logs-to-cambium-logger-at-level-with-type-context-and-correct-meta
  (doseq [{:keys [slf4j-level-name without-opts]} defs/level-defs]
    (let [{:keys [fn meta]} without-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}]
      (cartus-cambium/initialise)

      (fn logger type context)

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
    (let [{:keys [fn meta]} with-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}
          message "Some event just happened."]
      (cartus-cambium/initialise)

      (fn logger type context
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
    (let [{:keys [fn meta]} with-opts
          {:keys [line column]} meta
          logger (cartus-cambium/logger)
          log-output-stream (logback/configure)
          type ::some.event
          context {:some "context"}
          exception (ex-info "Something went wrong..." {:some "data"})]
      (fn logger type context {:exception exception})

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
