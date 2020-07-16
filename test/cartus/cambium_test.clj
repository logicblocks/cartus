(ns cartus.cambium-test
  (:require
   [clojure.test :refer :all]
   [clojure.string :as string]

   [cheshire.core :as json]

   [cartus.test-support.definitions :as defs]
   [cartus.test-support.logback :as logback]

   [cartus.cambium :as cartus-cambium]
   [cartus.core :as cartus]))

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

(deftest adds-global-context-to-cambium-logger-with-local-context-priority
  (doseq [{:keys [without-opts]} defs/level-defs]
    (let [{:keys [log-fn]} without-opts
          local-context {:first 10
                         :second 20}
          global-context {:first 1
                          :third 30}

          cambium-logger (cartus-cambium/logger)
          amended-logger (cartus/with-global-context
                           cambium-logger global-context)

          log-output-stream (logback/configure)

          type ::some.event]
      (cartus-cambium/initialise)

      (log-fn amended-logger type local-context)

      (let [log-line (first (log-lines log-output-stream))]
        (is (= 10 (:first log-line)))
        (is (= 20 (:second log-line)))
        (is (= 30 (:third log-line)))))))
