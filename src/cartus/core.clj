(ns cartus.core)

(defprotocol Logger
  (log [this level type context
        {:keys [message exception meta]
         :or   {meta {}}
         :as   opts}]))

(defrecord GlobalContextLogger [delegate global-context]
           Logger
           (log [_ level type context opts]
             (log delegate level type (merge global-context context) opts)))

(defn with-global-context [logger context]
  (map->GlobalContextLogger
    {:delegate       logger
     :global-context context}))

(defmacro ^:private deflevel
  "This macro is used internally to define namespace-based level loggers."
  [level-symbol]
  (let [level-keyword (keyword level-symbol)
        level-doc (str "Log event at " level-symbol " level.")
        arglists ''([logger type context] [logger type context opts])]

    `(defmacro ~level-symbol
       ~level-doc
       {:arglists ~arglists}

       ([logger# type# context#]
        (with-meta
          `(log ~logger# ~~level-keyword ~type# ~context#
             {:meta (merge {:ns ~*ns*} ~~'(meta &form))})
          ~'(meta &form)))

       ([logger# type# context# opts#]
        (with-meta
          `(log ~logger# ~~level-keyword ~type# ~context#
             (merge ~opts# {:meta (merge {:ns ~*ns*} ~~'(meta &form))}))
          ~'(meta &form))))))

(declare
  trace
  debug
  info
  warn
  error
  fatal)

(deflevel trace)
(deflevel debug)
(deflevel info)
(deflevel warn)
(deflevel error)
(deflevel fatal)
