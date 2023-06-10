# Getting Started

`cartus` is a structured logging abstraction for logging data rich events with 
support for multiple logging backends, currently including:

* a test logger for collecting logs in memory and asserting against 
  them;
* a [`cambium`](https://cambium-clojure.github.io/) logger for logging 
  out via [`SLF4J`](http://www.slf4j.org/) and 
  [`logback`](http://logback.qos.ch/); and
* a null logger that ignores all logged events, useful as a default 
  implementation or to turn off logging completely.
         
`cartus` is heavily inspired by 
[JUXT's blog post on logging](https://juxt.pro/blog/logging) which recommends 
treating the logger as an injectable dependency like any other. This brings a 
number of benefits:

* Testing for log events becomes easy, since the logger implementation can be
  switched out for one more amenable to testing.
* More generally, different logging backends can be supported in different 
  contexts.
* Since the logger is passed as an explicit dependency, it can be transformed
  in various ways, such as by adding context to all log events or filtering log 
  events to only those of a certain set of types within a particular scope.
  
## Contents

- [Installation](#installation)
- [Configuring a backend](#configuring-a-backend)
    - [The `cartus.test/logger` backend](#test-backend)
    - [The `cartus.cambium/logger` backend](#cambium-backend)
    - [The `cartus.null/logger` backend](#null-backend)
    - [Custom backends](#custom-backends)
- [Logging events](#logging-events)
- [Applying transformations to loggers](#applying-transformations-to-loggers)
    - [Setting logger context](#setting-logger-context)
    - [Filtering by log levels](#filtering-by-log-levels)
    - [Filtering by log types](#filtering-by-log-types)
    - [Applying arbitrary transformations](#applying-arbitrary-transformations)
- [Testing for log events](#testing-for-log-events)
    - [Asserting directly against logged events](#asserting-directly-against-logged-events)
    - [Using the `logged?` assertion](#logged?-assertion)
    - [Using modifiers with `logged?`](#using-modifiers)
    - [Advanced usage of `logged?`](#advanced-usage)

## Installation

Add the following to your `project.clj` file:

```clojure
[io.logicblocks/cartus.core "0.1.18"]
```

## Configuring a backend

### <a name="test-backend"></a>The `cartus.test/logger` backend

The [[cartus.test/logger]] backend captures all logged events in memory in an
atom, allowing your tests to assert that log events took place.

To install the `cartus.test/logger` backend, add the following to your 
`project.clj` file:

```clojure
[io.logicblocks/cartus.test "0.1.18"]
```

To create a `cartus.test/logger`:

```clojure
(require '[cartus.test])

(def logger (cartus.test/logger))
```

### <a name="cambium-backend"></a>The `cartus.cambium/logger` backend

The [[cartus.cambium/logger]] backend passes all logged events to `cambium`
which in turn uses [`SLF4J`](http://www.slf4j.org/) and 
[`logback`](http://logback.qos.ch/) to log out the log event either in a plain 
text format or as JSON.

To install the `cartus.cambium/logger` backend, add the following to your 
`project.clj` file:

```clojure
[io.logicblocks/cartus.cambium "0.1.18"]
```

Additionally, you must choose a codec and backend for `cambium`, which has 
implementations for plain text logging and for JSON logging. 

To use plain text logs, add the following to your `project.clj` file:

```clojure
[cambium/cambium.codec-simple "0.9.3"]
```

and add logback configuration (typically at `resources/logback.xml`) containing
something like the following:

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>
        %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg { %mdc }%n
      </pattern>
    </encoder>
  </appender>
  <root level="debug">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

To use JSON logs, add the following to your `project.clj` file:

```clojure
[cambium/cambium.codec-cheshire "0.9.3"]
[cambium/cambium.logback.json   "0.4.3"]
```

and add logback configuration (again, typically at `resources/logback.xml`)
containing something like the following:

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="cambium.logback.json.FlatJsonLayout">
        <jsonFormatter class="ch.qos.logback.contrib.jackson.JacksonJsonFormatter">
          <prettyPrint>true</prettyPrint>
        </jsonFormatter>
        <timestampFormat>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampFormat>
        <timestampFormatTimezoneId>UTC</timestampFormatTimezoneId>
        <appendLineSeparator>true</appendLineSeparator>
      </layout>
    </encoder>
  </appender>
  <root level="debug">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

For more details of configuring `cambium` backends, see the 
[`cambium` documentation](https://cambium-clojure.github.io/).

Once a `cambium` codec and backend has been configured, `cambium` must be 
initialised as the system starts up. This can be achieved with the following:

```clojure
(require '[cartus.cambium])

(cartus.cambium/initialise)
```

See [[cartus.cambium/initialise]] for more details on initialisation options.

Once `cambium` is initialised, to create a `cartus.cambium/logger`:

```clojure
(def logger (cartus.cambium/logger))
```

### <a name="null-backend"></a>The `cartus.null/logger` backend

The [[cartus.null/logger]] backend ignores all logged events which can be 
useful to turn off logging completely or provide a default implementation in
the case that one is not provided.

To install the `cartus.null/logger` backend, add the following to your 
`project.clj` file:

```clojure
[io.logicblocks/cartus.null "0.1.18"]
```

To create a `cartus.null/logger`:

```clojure
(require '[cartus.null])

(def logger (cartus.null/logger))
```

### Custom backends

All `cartus.core` functions expect an implementation of the 
[[cartus.core/Logger]] protocol. 

For example, to create a naive `StdoutLogger` which prints all log events to the 
standard output stream as EDN:

```clojure
(ns example.stdout
  (:require
   [cartus.core :as core]))

(defrecord StdoutLogger
  []
  core/Logger
  (log [_ level type context opts]
    (println 
      (merge {:level level
            :type type
            :context context}
        opts))))

(defn logger []
  (map->StdoutLogger {}))
```

## Logging events

Log events:

- are logged at a specific `level`, typically one of `:trace`, `:debug`,
  `:info`, `:warn`, `:error` or `:fatal`;
- have a `type`, expressed as a keyword, such as
  `:some.namespace/some.event.type`;
- include a map of additional `context`, which can be deeply nested;
- optionally include a `message` giving a textual description of the event;
- optionally include an `exception` including more details of an error;
- optionally include a `meta` map, including any metadata at the point of
  the call to the logger such as the line, column and
  namespace of the call.

To log events at each respective level, you can use the convenience macros 
[[cartus.core/trace]], [[cartus.core/debug]], [[cartus.core/info]], 
[[cartus.core/warn]], [[cartus.core/error]] and [[cartus.core/fatal]], 
which also capture metadata for the call site automatically: 

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def logger (cartus.cambium/logger))

(log/trace logger ::system.started 
  {:flags {:in-memory-database false
           :live-services false}})
; => 
; "{
;   \"timestamp\" : \"2020-07-18T16:59:36.625Z\",
;   \"level\" : \"TRACE\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 10,
;   \"column\" : 1,
;   \"flags\" : {
;     \"in-memory-database\" : false,
;     \"live-services\" : false
;   },
;   \"type\" : \"example.logging/system.started\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/system.started\",
;   \"context\" : \"default\"
; }
; "

(log/info logger ::database.migrating
  {:target-schema-version 10})
; => 
; "{
;   \"timestamp\" : \"2020-07-18T17:00:54.954Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 32,
;   \"column\" : 1,
;   \"target-schema-version\" : 10,
;   \"type\" : \"example.logging/database.migrating\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/database.migrating\",
;   \"context\" : \"default\"
; }
; "

(log/debug logger ::database.migrated
  {:current-schema-version 10 :elapsed-millis 568}
  {:message "Success!"})
; =>
; "{
;   \"timestamp\" : \"2020-07-18T17:02:10.798Z\",
;   \"level\" : \"DEBUG\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 50,
;   \"current-schema-version\" : 10,
;   \"column\" : 1,
;   \"elapsed-millis\" : 568,
;   \"type\" : \"example.logging/database.migrated\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"Success!\",
;   \"context\" : \"default\"
; }

(log/error logger ::cache.connecting
  {:address "tcp://cache.example.com:3456"}
  {:exception (ex-info "Connection failed" {:timeout-millis 500})})
; =>
; "{
;   \"timestamp\" : \"2020-07-18T17:03:13.756Z\",
;   \"level\" : \"ERROR\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"address\" : \"tcp://cache.example.com:3456\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 69,
;   \"column\" : 1,
;   \"type\" : \"example.logging/cache.connecting\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/cache.connecting\",
;   \"context\" : \"default\",
;   \"exception\" : \"clojure.lang.ExceptionInfo: Connection failed\
; \\tat example.logging$eval6273.invokeStatic(form-init17428929335843016668.clj:3)\
; \\tat example.logging$eval6273.invoke(form-init17428929335843016668.clj:1)\
; \\tat clojure.lang.Compiler.eval(Compiler.java:7177)\
; \\tat clojure.lang.Compiler.eval(Compiler.java:7132)\
; \\tat clojure.core$eval.invokeStatic(core.clj:3214)\
; \\tat clojure.core$eval.invoke(core.clj:3210)\
; \\tat nrepl.middleware.interruptible_eval$evaluate$fn__3914.invoke(interruptible_eval.clj:91)\
; \\tat clojure.main$repl$read_eval_print__9086$fn__9089.invoke(main.clj:437)\
; \\tat clojure.main$repl$read_eval_print__9086.invoke(main.clj:437)\
; \\tat clojure.main$repl$fn__9095.invoke(main.clj:458)\
; \\tat clojure.main$repl.invokeStatic(main.clj:458)\
; \\tat clojure.main$repl.doInvoke(main.clj:368)\
; \\tat clojure.lang.RestFn.invoke(RestFn.java:1523)\
; \\tat nrepl.middleware.interruptible_eval$evaluate.invokeStatic(interruptible_eval.clj:84)\
; \\tat nrepl.middleware.interruptible_eval$evaluate.invoke(interruptible_eval.clj:56)\
; \\tat nrepl.middleware.interruptible_eval$interruptible_eval$fn__3940$fn__3944.invoke(interruptible_eval.clj:155)\
; \\tat clojure.lang.AFn.run(AFn.java:22)\
; \\tat nrepl.middleware.session$session_exec$main_loop__4041$fn__4045.invoke(session.clj:190)\
; \\tat nrepl.middleware.session$session_exec$main_loop__4041.invoke(session.clj:189)\
; \\tat clojure.lang.AFn.run(AFn.java:22)\
; \\tat java.lang.Thread.run(Thread.java:748)\
; \"
; }
; "

(log/fatal logger ::exception.unknown)
; =>
; "{
;   \"timestamp\" : \"2020-07-18T17:06:22.932Z\",
;   \"level\" : \"ERROR\",
;   \"thread\" : \"nREPL-session-f070aa75-a886-4207-83f8-65f73c0ba556\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 50,
;   \"column\" : 1,
;   \"type\" : \"example.logging/exception.unknown\",
;   \"logger\" : \"example.logging\",
;   \"context\" : \"default\"
; }
``` 

Alternatively, you can use the `cartus.core/log` function from 
[[cartus.core/Logger]] to log directly, giving full control over all aspects of 
the log event:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def logger (cartus.cambium/logger))

(log/log logger :info ::system.started
  {:flags {:in-memory-database false
             :live-services false}}
  {:meta {:line 10 :column 20 :ns (find-ns 'example.other)}})
; =>
; "{
;   \"timestamp\" : \"2020-07-18T17:35:50.896Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"ns\" : \"example.other\",
;   \"line\" : 10,
;   \"column\" : 20,
;   \"flags\" : {
;     \"in-memory-database\" : false,
;     \"live-services\" : false
;   },
;   \"type\" : \"example.logging/system.started\",
;   \"logger\" : \"example.other\",
;   \"message\" : \"example.logging/system.started\",
;   \"context\" : \"default\"
; }
; "
```

Note that in the case you use `cartus.core/log` directly, no metadata is 
captured so metadata must be provided explicitly.

## Applying transformations to loggers

Since `cartus.core` functions accept the logger as an explicit dependency, you
can easily apply transformations to the logger as it is passed down through
function calls. This allows additional behaviour to be added to a logger in a
scoped manner, irrespective of backend used.

### Setting logger context

To add logger context to a logger instance, use [[cartus.core/with-context]]. 
This function returns a new logger which will merge the provided context map 
with the context map provided at log time, with the log time context taking 
preference:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def contextual-logger (cartus.core/with-context standard-logger
                        {:request-id 5 :user-id 15}))

(log/info contextual-logger ::service.requesting
  {:request-id 10 :endpoint-name "check"})
; =>
; "{
;   \"timestamp\" : \"2020-07-18T17:51:35.862Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-ea7676d2-b76c-44c4-bc2b-d1c2f8515c0b\",
;   \"endpoint-name\" : \"check\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 12,
;   \"user-id\" : 15,
;   \"column\" : 1,
;   \"request-id\" : 10,
;   \"type\" : \"example.logging/service.requesting\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/service.requesting\",
;   \"context\" : \"default\"
; }
; "
```

### Filtering by log levels

To retain log events based on their level, use 
[[cartus.core/with-levels-retained]]. This function returns a new logger which 
will drop any log events that do not match the specified criteria based on a 
couple of different variants as discussed below.

To retain log events having a level within a provided set of levels:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger (cartus.core/with-levels-retained standard-logger
                        #{:warn :error}))

(log/info filtered-logger ::service.requesting
  {:request-id 10 :endpoint-name "check"})
; => nothing logged

(log/warn filtered-logger ::service.slow
  {:latency-millis 5486})
; "{
;   \"timestamp\" : \"2020-07-19T15:21:42.436Z\",
;   \"level\" : \"WARN\",
;   \"thread\" : \"nREPL-session-796b74ce-6705-4cf7-a55c-d95b089b8b34\",
;   \"latency-millis\" : 5486,
;   \"ns\" : \"example.logging\",
;   \"line\" : 15,
;   \"column\" : 1,
;   \"type\" : \"example.logging/service.slow\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/service.slow\",
;   \"context\" : \"default\"
; }
; "
```

To retain log events having a level greater than or equal to a specified level
in severity:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger 
  (cartus.core/with-levels-retained standard-logger >= :info))

(log/debug filtered-logger ::database.connection-pool.requesting
  {:timeout-millis 200})
; => nothing logged

(log/info filtered-logger ::database.querying
  {:query-name :find-user})
; "{
;   \"timestamp\" : \"2020-07-19T15:28:23.843Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-796b74ce-6705-4cf7-a55c-d95b089b8b34\",
;   \"query-name\" : "find-user",
;   \"ns\" : \"example.logging\",
;   \"line\" : 15,
;   \"column\" : 1,
;   \"type\" : \"example.logging/database.querying\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/database.querying\",
;   \"context\" : \"default\"
; }
; "
```

The arity-3 version of [[cartus.core/with-levels-retained]] accepts the 
operators `>=`, `>`, `=`, `<` and `<=`.

To ignore log events based on their level, use 
[[cartus.core/with-levels-ignored]]. This function returns a new logger which 
will drop any log events that match the specified criteria based on a 
couple of different variants as discussed below.

To ignore log events having a level within a provided set of levels:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger (cartus.core/with-levels-ignored standard-logger
                        #{:warn :error}))

(log/info filtered-logger ::service.requesting
  {:request-id 10 :endpoint-name "check"})
; =>
; "{
;   \"timestamp\" : \"2020-07-19T17:35:12.894Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-d8857a81-72f5-41ce-a107-eaecc7b75805\",
;   \"request-id\" : 10,
;   \"endpoint-name\" : \"check\",
;   \"ns\" : \"example.logging\",
;   \"line\" : 12,
;   \"column\" : 1,
;   \"type\" : \"example.logging/service.requesting\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/service.requesting\",
;   \"context\" : \"default\"
; }
; "

(log/warn filtered-logger ::service.slow
  {:latency-millis 5486})
; => nothing logged
```

To ignore log events having a level less than or equal to a specified level
in severity:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger 
  (cartus.core/with-levels-ignored standard-logger < :info))

(log/debug filtered-logger ::database.connection-pool.requesting
  {:timeout-millis 200})
; => nothing logged

(log/info filtered-logger ::database.querying
  {:query-name :find-user})
; "{
;   \"timestamp\" : \"2020-07-19T17:48:13.983Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-b8bbb4af-f967-49eb-8ef3-9ad8f93376e8\",
;   \"query-name\" : "find-user",
;   \"ns\" : \"example.logging\",
;   \"line\" : 15,
;   \"column\" : 1,
;   \"type\" : \"example.logging/database.querying\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/database.querying\",
;   \"context\" : \"default\"
; }
; "
```

The arity-3 version of [[cartus.core/with-levels-ignored]] accepts the 
operators `>=`, `>`, `=`, `<` and `<=`.

### Filtering by log types

To retain log events based on their type, use 
[[cartus.core/with-types-retained]]. This function returns a new logger which 
will drop any log events not having a type in the provided set of types:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger (cartus.core/with-types-retained standard-logger
                        #{::order.rejected ::order.approved}))

(log/debug filtered-logger ::order.pending
  {:outstanding-steps [:payment]})
; => nothing logged

(log/info filtered-logger ::order.rejected
  {:reason :card-payment-failed})
; "{
;   \"timestamp\" : \"2020-07-19T18:56:22.674Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-5d899bb7-3d16-4d84-81e0-691fc2df2c66\",
;   \"reason\" : "card-payment-failed",
;   \"ns\" : \"example.logging\",
;   \"line\" : 15,
;   \"column\" : 1,
;   \"type\" : \"example.logging/order.rejected\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/order.rejected\",
;   \"context\" : \"default\"
; }
; "
```

To ignore log events based on their type, use 
[[cartus.core/with-types-ignored]]. This function returns a new logger which 
will drop any log events having a type in the provided set of types:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def filtered-logger (cartus.core/with-types-ignored standard-logger
                        #{::order.pending}))

(log/debug filtered-logger ::order.pending
  {:outstanding-steps [:payment]})
; => nothing logged

(log/info filtered-logger ::order.rejected
  {:reason :card-payment-failed})
; "{
;   \"timestamp\" : \"2020-07-19T18:57:23.682Z\",
;   \"level\" : \"INFO\",
;   \"thread\" : \"nREPL-session-5d899bb7-3d16-4d84-81e0-691fc2df2c66\",
;   \"reason\" : "card-payment-failed",
;   \"ns\" : \"example.logging\",
;   \"line\" : 15,
;   \"column\" : 1,
;   \"type\" : \"example.logging/order.rejected\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/order.rejected\",
;   \"context\" : \"default\"
; }
; "
```

### Applying arbitrary transformations

More generally, a logger can be seen as a stream of log events. With this in 
mind, `cartus` supports transducers being applied to loggers. This allows 
arbitrarily complex transformations to be built up using Clojure's existing 
transducer support.

For example, to remove events that contain sensitive information and then map 
all context keys to snake case before logging an event, you can use the 
following: 

```clojure
(ns example.logging
  (:require
   [clojure.string :as string]
   [clojure.walk :as walk]

   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(defn transform-keys
  [t coll]
  (let [f (fn [[k v]] [(t k) v])]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) coll)))

(defn ->camel-case [k]
  (keyword 
    (string/replace (name k) #"-(\w)"
      #(string/upper-case (second %1)))))

(def standard-logger (cartus.cambium/logger))
(def filtered-logger 
  (cartus.core/with-transformation standard-logger
    (comp 
      (filter #(not (contains? (:context %) :password)))
      (map #(assoc % :context (transform-keys ->camel-case (:context %)))))))

(log/debug filtered-logger ::order.pending
  {:outstanding-steps [:payment]
   :line-items [:item-1 :item-2]})
; "{
;   \"timestamp\" : \"2020-07-23T08:51:14.165Z\",
;   \"level\" : \"DEBUG\",
;   \"thread\" : \"nREPL-session-86b79ffc-6ce9-43af-91a9-0b1de812e86c\",
;   \"lineItems\" : [\"item-1\",\"item-2\"],
;   \"ns\" : \"example.logging\",
;   \"line\" : 1,
;   \"column\" : 1,
;   \"outstandingSteps\" : [\"payment\"],
;   \"type\" : \"example.logging/order.pending\",
;   \"logger\" : \"example.logging\",
;   \"message\" : \"example.logging/order.pending\",
;   \"context\" : \"default\"
; }
; "

(log/info filtered-logger ::registration.in-progress
  {:password "super-secret"})
; => nothing logged
```

All other transformations in [[cartus.core]] use the transducer support under
the covers and can be used as examples when implementing your own.

## Testing for log events

With the help of the [[cartus.test/logger]] and utilities in the 
[[cartus.test]], namespace you can assert that log events occurred from within
your tests.

### Asserting directly against logged events

```clojure
(ns example.subject
  (:require
   [cartus.core :as log]))

(defn +-with-logging [logger & vals]
  (log/info logger ::summing.values {:values vals})
  (apply + vals))

(ns example.subject-test
  (:require
   [clojure.test :refer :all]

   [cartus.test :as log-test]
   
   [example.subject :as subject]))

(deftest logs-while-summing
  (let [logger (log-test/logger)]
    (subject/+-with-logging logger 1 2 3)
    (is (= [{:level   :info
             :type    :example.subject/summing.values
             :context {:values [1 2 3]}
             :meta    {:ns (find-ns 'example.subject)
                       :line 6
                       :column 3}}]
      (log-test/events logger)))))
; => passes
``` 

### <a name="logged?-assertion"></a>Using the `logged?` assertion

Asserting directly against log events returned by [[cartus.test/events]] can be 
cumbersome. To make log assertions more intentional, use the 
[[cartus.test/logged?]] assertion, which implements the 
`clojure.test/assert-expr` multimethod, extending `clojure.test/is` to respond
to a `logged?` form:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    (subject/+-with-logging logger 1 2 3)
    (is (logged? logger 
          {:level   :info
           :type    :example.subject/summing.values
           :context {:values [1 2 3]}
           :meta    {:ns (find-ns 'example.subject)
                     :line 6
                     :column 3}}))))
; => passes
```

### <a name="using-modifiers"></a>Using modifiers with `logged?`

By default, `logged?` matches fuzzily against the events logged to the logger.
As a result, log specs don't need to include all attributes and can instead 
include a subset:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    (subject/+-with-logging logger 1 2 3)
    (is (logged? logger 
          {:level   :info
           :type    :example.subject/summing.values
           :context {:values [1 2 3]}}))))
; => passes
```

This is useful when the metadata of the logged event is unimportant or where
you don't care about some part of the logged event, say, the level at which the
event was logged.

If instead you want to match strictly against the events logged to the logger,
pass the `:strict-contents` modifier:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    (subject/+-with-logging logger 1 2 3)
    (is (logged? logger #{:strict-contents}
          {:level   :info
           :type    :example.subject/summing.values
           :context {:values [1 2 3]}}))))
; => fails, due to missing `:meta` attribute
```

The `logged?` assertion accepts a variable number of log specs, to assert 
against a sequence of log events. By default, `logged?` expects the order of
log events to match the log specs but allows surplus log events in the log 
stream and allows log events to occur between those events specified by the 
provided log specs. As such, it is enough for matching log events to appear
in the correct order somewhere in the log stream:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    
    (subject/+-with-logging logger 1 2 3)
    (subject/+-with-logging logger 4 5 6)
    (subject/+-with-logging logger 7 8 9)
    (subject/+-with-logging logger 10 11 12)
    (subject/+-with-logging logger 13 14 15)

    (is (logged? logger 
          {:type    :example.subject/summing.values
           :context {:values [4 5 6]}}
          {:type    :example.subject/summing.values
           :context {:values [10 11 12]}}))))
; => passes
```

If instead, you want to assert that a set of log events occur, but the order is
not important, pass the `:in-any-order` modifier:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    
    (subject/+-with-logging logger 1 2 3)
    (subject/+-with-logging logger 4 5 6)
    (subject/+-with-logging logger 7 8 9)
    (subject/+-with-logging logger 10 11 12)
    (subject/+-with-logging logger 13 14 15)

    (is (logged? logger #{:in-any-order} 
          {:type    :example.subject/summing.values
           :context {:values [10 11 12]}}
          {:type    :example.subject/summing.values
           :context {:values [4 5 6]}}))))
; => passes
```

This can be useful when concurrent processes are logging to the same logger
since event ordering in such a case is not guaranteed.

Sometimes, you might want to assert that a strict sequence of log events has
occurred and don't want to tolerate surplus log events. To assert that only
the specified log events have occurred, use the `:only` modifier:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    
    (subject/+-with-logging logger 1 2 3)
    (subject/+-with-logging logger 4 5 6)
    (subject/+-with-logging logger 7 8 9)
    (subject/+-with-logging logger 10 11 12)
    (subject/+-with-logging logger 13 14 15)

    (is (logged? logger #{:only} 
          {:type    :example.subject/summing.values
           :context {:values [4 5 6]}}
          {:type    :example.subject/summing.values
           :context {:values [10 11 12]}}))))
; => fails, since there are many surplus log events
```

Again, the default behaviour is that log events are expected in the order in
which they are specified. If instead you want to assert that only the specified
log events have occurred but where order is not important, use both the `:only`
and `:in-any-order` modifiers:

```clojure
(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    
    (subject/+-with-logging logger 1 2 3)
    (subject/+-with-logging logger 4 5 6)
    
    (is (logged? logger #{:only :in-any-order} 
          {:type    :example.subject/summing.values
           :context {:values [4 5 6]}}
          {:type    :example.subject/summing.values
           :context {:values [1 2 3]}}))))
; => passes
```

The available modifiers are:

  - `#{:in-order :in-any-order}` defining ordering constraints
  - `#{:only :at-least}` defining cardinality constraints
  - `#{:fuzzy-contents :strict-contents}` defining content constraints
  
and any combination of one from each set is permitted. The effective default
modifiers are: `#{:in-order :at-least :fuzzy-contents}`.

### <a name="advanced-usage"></a>Advanced usage of `logged?`

Internally, the `logged?` assertion uses the 
[`matcher-combinators`](https://github.com/nubank/matcher-combinators) library
which allows for more advanced matching. Specifically, `matcher-combinators`
supports predicate matchers as well as more targetted collection matching.

For example, to ensure that a log event occurs with one of a number of types,
you can do the following:

```clojure
(is (logged? logger
      {:level :info
       :type #(or (= :some.ns/type-1 %) (= :some.ns/type-2 %))}))
``` 

This will assert that at least a log event with a type of `:some.ns/type-1` or
`:some.ns/type-2` occurs at log level `:info`.

Predicates can occur deep inside a nested log spec, for example:

```clojure
(is (logged? logger
      {:level :info
       :type  :some.ns/type-1
       :context {:factors [#(> % 3) #(> % 5) 8]}}))
``` 

In addition to predicate matchers, `matcher-combinators` allows you to more
tightly or loosely control the make up of collection types. By default, maps
are matched using a matcher called `embeds` which allows surplus attributes
to be present. If you wish to exactly match a map in the log event context,
whilst retaining fuzzy matching on the log event overall you can do the 
following:

```clojure
(require '[matcher-combinators.matchers :as mc-matchers])

(is (logged? logger
      {:level :info
       :type  :some.ns/type-1
       :context {:reference 
                 (mc-matchers/equals 
                   {:id "123"
                    :region "us"
                    :account-id "456"})}}))
``` 

Internally, the `logged?` modifiers are implemented as combinations of different
`matcher-combinator` matchers, allowing for more flexible log specs than 
provided by the modifiers alone.

For more details on how to compose more complex matchers, see the
[`matcher-combinator` documentation](https://cljdoc.org/d/nubank/matcher-combinators).
