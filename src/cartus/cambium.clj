(ns cartus.cambium
  "A [[cartus.core/Logger]] implementation that delegates all logged events to
  [cambium](https://cambium-clojure.github.io/)."
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as ctl]
   [clojure.tools.logging.impl :as ctl-impl]

   [cambium.core :as cambium]
   [cambium.logback.json.flat-layout :as cambium-flat-layout]

   [cartus.core :as cartus])
  (:import
   [org.slf4j.bridge SLF4JBridgeHandler]))

(defn initialise
  "Initialises cambium and logback.

  Specifically:

    - configures the decoder for the chosen codec onto the cambium layout class;
    - configures the transformer, when provided, onto the cambium layout class;
      and
    - configures java.util.logging to log through SLF4J.

  When no argument is provided, no transformer is configured.

  See the [cambium documentation](https://cambium-clojure.github.io/documentation.html)
  for further details."
  ([] (initialise {}))
  ([{:keys [decoder transformer]
     :or   {decoder cambium.codec/destringify-val}}]
   (do
     (when decoder (cambium-flat-layout/set-decoder! decoder))
     (when transformer (cambium-flat-layout/set-transformer! transformer))
     (SLF4JBridgeHandler/removeHandlersForRootLogger)
     (SLF4JBridgeHandler/install))))

(deftype CambiumLogger
  []
  cartus/Logger
  (log [_ level type context
        {:keys [message exception meta]
         :or   {meta {}}}]
    (let [logger
          (ctl-impl/get-logger ctl/*logger-factory* (get meta :ns *ns*))
          context (merge context {:type type} meta)
          message (or message (string/join "/" ((juxt namespace name) type)))]
      (cambium/log logger level context exception message))))

(defn logger
  "Constructs a cambium logger logging all events through cambium.

  Whilst `cartus.cambium` includes the core cambium and logback dependencies,
  in order to use this logger, additional cambium dependencies must be included.
  These dependencies control the codec used for encoding and decoding
  structured data and the backend used to output the event. Additionally,
  logback appenders must be configured to use cambium.

  See the [cambium documentation](https://cambium-clojure.github.io/documentation.html)
  for further details.

  Once a codec and backend have been chosen and configured, cambium must be
  initialised before any events are logged. This can be achieved with
  [[initialise]].

  Since SLF4J requires a message on all log events, the resulting logger will
  use the event type as the message by default, with the type also appearing
  on the logged event under the `\"type\"` key. An explicitly passed message
  overrides this behaviour."
  []
  (new CambiumLogger))
