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

(defrecord TransformerLogger
  [xform delegate]
  Logger
  (log [_ level type context opts]
    ((xform
       (fn [logger event]
         (log logger
           (:level event)
           (:type event)
           (:context event)
           (:opts event))))
     delegate
     {:level level :type type :context context :opts opts})))

(defn with-transformation
  "Returns a new logger which applies the provided transducer to any logged
  event before passing it on to the underlying logger.

  The transducer will receive the logged event as a map with keys `:level`,
  `:type`, `:context` and `:opts` set to the corresponding arguments to the
  log function and should return a map of the same shape."
  [logger xform]
  (map->TransformerLogger
    {:delegate logger
     :xform    xform}))

(defn with-context
  "Returns a new logger which merges the provided context map into that of any
  subsequent log call.

  Entries in the context map provided at log time takes precedence over
  entries in the context map passed to this function. The applied merge is
  shallow."
  [logger context]
  (with-transformation
    logger
    (map
      (fn [event]
        (assoc event :context (merge context (:context event)))))))

(defrecord CompositeLogger
  [loggers]
  Logger
  (log [_ level type context opts]
    (doseq [logger loggers]
      (log logger level type context opts))))

(defn compose-loggers
  "Returns a new logger which logs to each of the provided loggers"
  [logger-a logger-b & other-loggers]
  (let [loggers (concat [logger-a logger-b] other-loggers)]
    (map->CompositeLogger {:loggers loggers})))

(def ^:private all-levels
  [:trace :debug :info :warn :error :fatal])

(defmulti ^:private filtered-levels
  (fn [_ operator _] operator))
(defmethod ^:private filtered-levels >= [levels _ level]
  (drop-while #(not= % level) levels))
(defmethod ^:private filtered-levels > [levels _ level]
  (drop 1 (drop-while #(not= % level) levels)))
(defmethod ^:private filtered-levels = [_ _ level]
  [level])
(defmethod ^:private filtered-levels < [levels _ level]
  (take-while #(not= % level) levels))
(defmethod ^:private filtered-levels <= [levels _ level]
  (conj (take-while #(not= % level) levels) level))

(defn with-levels-retained
  "Returns a new logger which retains log events matching the provided criteria.

  The arity-2 version expects a logger and a seq of levels to ignore.
  The arity-3 version expects a logger, an operator and a level. Supported
  operators are <=, <, =, >, >= passed as symbols."
  ([logger operator level]
   (with-levels-retained logger
     (filtered-levels all-levels operator level)))
  ([logger levels]
   (with-transformation
     logger
     (filter
       (fn [event]
         ((set levels) (:level event)))))))

(defn with-levels-ignored
  "Returns a new logger which ignores log events matching the provided criteria.

  The arity-2 version expects a logger and a seq of levels to ignore.
  The arity-3 version expects a logger, an operator and a level. Supported
  operators are <=, <, =, >, >= passed as symbols."
  ([logger operator level]
   (with-levels-ignored logger
     (filtered-levels all-levels operator level)))
  ([logger levels]
   (with-transformation
     logger
     (remove
       (fn [event]
         ((set levels) (:level event)))))))

(defn with-types-retained
  "Returns a new logger which retains log events having one of the provided
  types."
  [logger types]
  (with-transformation
    logger
    (filter
      (fn [event]
        ((set types) (:type event))))))

(defn with-types-ignored
  "Returns a new logger which ignores log events having any of the provided
  types."
  [logger types]
  (with-transformation
    logger
    (remove
      (fn [event]
        ((set types) (:type event))))))

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
        arglists ''([logger type]
                    [logger type context]
                    [logger type context opts])]

    `(defmacro ~level-symbol
       ~level-doc
       {:arglists ~arglists}

       ([logger# type# context# opts#]
        (with-meta
          `(log ~logger# ~~level-keyword ~type# ~context#
             (merge ~opts# {:meta (merge {:ns ~*ns*} ~~'(meta &form))}))
          ~'(meta &form)))

       ([logger# type# context#]
        (with-meta
          `(log ~logger# ~~level-keyword ~type# ~context#
             {:meta (merge {:ns ~*ns*} ~~'(meta &form))})
          ~'(meta &form)))

       ([logger# type#]
        (with-meta
          `(log ~logger# ~~level-keyword ~type# {}
             {:meta (merge {:ns ~*ns*} ~~'(meta &form))})
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
