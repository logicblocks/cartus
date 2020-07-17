(ns cartus.core
  "A structured logging abstraction for logging data rich events with support
  for multiple backends.

  The `cartus.core` namespace includes core protocols, macros and utilities
  for managing loggers.

  `cartus` is heavily inspired by [JUXT's blog on logging](https://juxt.pro/blog/logging)
  which is worth a read to understand the motivations behind this library."
  (:require [clojure.string :as string]))

(defprotocol Logger
  "A protocol for logging events.

  Events:

    - are logged at a specific `level`, typically one of `:trace`, `:debug`,
      `:info`, `:warn`, `:error` or `:fatal`;
    - have a `type`, expressed as a keyword, such as
      `:some.namespace/some.event.type`;
    - include a map of additional `context`, which can be deeply nested;
    - optionally include a `message` where implementations are guided to use
      the `type` as the message if none is provided and logging implementations
      require a message;
    - optionally include an `exception` including more details of an error;
    - optionally include a `meta` map, including any metadata at the point of
      the call to the logger; the level specific macros [[trace]], [[debug]],
      [[info]], [[warn]], [[error]] and [[fatal]] include line, column and
      namespace metadata in log calls they delegate to the `Logger`.

  See [[cartus.test]] and [[cartus.cambium]] for example implementations."
  (log [this level type context
        {:keys [message exception meta]
         :or   {meta {}}
         :as   opts}]
    "Log the provided event."))

(defrecord GlobalContextLogger
  [delegate global-context]
  Logger
  (log [_ level type context opts]
    (log delegate level type (merge global-context context) opts)))

(defn with-global-context
  "Returns a new logger which includes the provided global context map in any
  subsequent log call.

  Entries in the local context map provided at log time takes precedence over
  entries in the global context map."
  [logger global-context]
  (map->GlobalContextLogger
    {:delegate       logger
     :global-context global-context}))

(defmacro ^:private deflevel
  "Used internally to define level specific logging macros that pass through
  call site metadata (at least line and column numbers) and capture calling
  namespace."
  [level-symbol]
  (let [level-keyword (keyword level-symbol)
        level-doc
        (string/join
          "\n"
          [(str
             "Log event at " level-symbol " level, capturing call site "
             "metadata.")
           ""
           "By default, forms include `:line` and `:column` metadata."
           "Additionally, this macro captures the calling namespace, with the "
           "metadata on the form taking precedence."
           ""
           "`logger`, `type`, `context` and `opts` are as defined on "
           "[[Logger]]."])
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
