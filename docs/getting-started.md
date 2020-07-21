# Getting Started

`cartus` is a structured logging abstraction for logging data rich events with 
support for multiple logging backends, currently including:

* a test logger for collecting logs in memory and asserting against 
  them; and
* a [`cambium`](https://cambium-clojure.github.io/) logger for logging 
  out via [`SLF4J`](http://www.slf4j.org/) and 
  [`logback`](http://logback.qos.ch/).
         
`cartus` is heavily inspired by 
[JUXT's blog post on logging](https://juxt.pro/blog/logging) which recommends 
treating the logger as an injectable dependency like any other. Among other 
things, this makes testing easier and allows the logging backend to be switched 
out for different contexts.

## Installation

Add the following to your `project.clj` file:

```clojure
[io.logicblocks/cartus "0.1.7"]
```

## Configuring a backend

### The `cartus.test/logger` backend

The [[cartus.test/logger]] backend captures all logged events in memory in an
atom, allowing your tests to assert that log events took place.

To create a `cartus.test/logger`:

```clojure
(require '[cartus.test])

(def logger (cartus.test/logger))
```

### The `cartus.cambium/logger` backend

The [[cartus.cambium/logger]] backend passes all logged events to `cambium`
which in turn uses [`SLF4J`](http://www.slf4j.org/) and 
[`logback`](http://logback.qos.ch/) to log out the log event either in a plain 
text format or as JSON.

To use the `cambium` logger, you must first choose a codec and backend for 
`cambium`. 

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

### Custom backends

All `cartus.core` functions expect an implementation of the 
[[cartus.core/Logger]] protocol. To create a custom backend, simply implement 
this protocol - the protocol's documentation gives full details of expectations
on implementors.

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

## Setting global context

Since `cartus.core` functions accept the logger as an explicit dependency, you
can easily add global context to a logger instance, using 
[[cartus.core/with-global-context]]. This function returns a new logger which
will merge the provided global context with the local context provided at log 
time, with the local context taking preference:

```clojure
(ns example.logging
  (:require
   [cartus.core :as log]
   [cartus.cambium]))

(cartus.cambium/initialise)

(def standard-logger (cartus.cambium/logger))
(def contextual-logger (cartus.core/with-global-context standard-logger
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

## Testing for log events

With the help of the [[cartus.test/logger]] and [[cartus.test/events]], you can 
assert that log events occurred from your tests:

```clojure
(ns example.subject
  (:require
   [cartus.core :as log]))

(defn +-with-logging [logger & vals]
  (log/info logger ::summing.values {:values vals})
  (apply + vals))

(ns example.subject-test
  (:require
   [cartus.test :as log-test]
   
   [example.subject :as subject]))

(deftest logs-while-summing
  (let [logger (cartus.test/logger)]
    (is (= 6 (subject/+-with-logging logger 1 2 3)))
    (is (= [{:level   :info
             :type    :example.subject/summing.values
             :context {:values [1 2 3]}
             :meta    {:ns (find-ns 'example.subject)
                       :line 6
                       :column 3}}]
      (log-test/events logger)))))
``` 
