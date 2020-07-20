(ns cartus.test-support.logback
  (:import
   [cambium.logback.json FlatJsonLayout]
   [ch.qos.logback.classic LoggerContext Level]
   [ch.qos.logback.contrib.jackson JacksonJsonFormatter]
   [ch.qos.logback.core OutputStreamAppender]
   [ch.qos.logback.core.encoder LayoutWrappingEncoder]
   [java.io ByteArrayOutputStream]
   [org.slf4j LoggerFactory Logger]
   [ch.qos.logback.classic.spi LoggingEvent ThrowableProxy]
   [ch.qos.logback.classic.pattern ThrowableProxyConverter]))

(defn configure
  ([] (configure (new ByteArrayOutputStream)))
  ([output-stream]
   (let [context
         (doto ^LoggerContext (LoggerFactory/getILoggerFactory)
           (.reset))

         json-formatter
         (doto (new JacksonJsonFormatter)
           (.setPrettyPrint false))

         layout
         (doto (new FlatJsonLayout)
           (.setContext context)
           (.setJsonFormatter json-formatter)
           (.setTimestampFormat "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
           (.setTimestampFormatTimezoneId "UTC")
           (.setAppendLineSeparator true)
           (.start))

         encoder
         (doto (new LayoutWrappingEncoder)
           (.setContext context)
           (.setLayout layout)
           (.start))

         appender
         (doto (new OutputStreamAppender)
           (.setContext context)
           (.setOutputStream output-stream)
           (.setEncoder encoder)
           (.setImmediateFlush true)
           (.start))

         root-logger
         (doto (.getLogger context Logger/ROOT_LOGGER_NAME)
           (.addAppender appender)
           (.setLevel Level/TRACE))]
     output-stream)))

(defn log-formatted-exception [throwable]
  (let [logging-event
        (doto (new LoggingEvent)
          (.setThrowableProxy (new ThrowableProxy throwable)))
        throwable-proxy-converter
        (doto (new ThrowableProxyConverter)
          (.start))]
    (.convert throwable-proxy-converter logging-event)))
