(ns cartus.cambium
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as ctl]
   [clojure.tools.logging.impl :as ctl-impl]

   [cambium.core :as cambium]
   [cambium.codec :as cambium-codec]
   [cambium.logback.json.flat-layout :as cambium-flat-layout]

   [cartus.core :as cartus])
  (:import
   [org.slf4j.bridge SLF4JBridgeHandler]))

(defn initialise []
  (do
    (cambium-flat-layout/set-decoder! cambium-codec/destringify-val)
    (SLF4JBridgeHandler/removeHandlersForRootLogger)
    (SLF4JBridgeHandler/install)))

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

(defn logger []
  (new CambiumLogger))
