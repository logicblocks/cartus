(defproject io.logicblocks/cartus.cambium "0.1.19-RC10"
  :description "A cambium backend for cartus."

  :plugins  [[lein-modules "0.3.11"]]

  :dependencies [[io.logicblocks/cartus.core :version]

                 [org.clojure/tools.logging "_"]

                 [cambium/cambium.core "_"]
                 [cambium/cambium.logback.core "_"]

                 [com.fasterxml.jackson.core/jackson-core "_"]
                 [com.fasterxml.jackson.core/jackson-databind "_"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "_"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "_"]

                 [org.slf4j/slf4j-api "_"]
                 [org.slf4j/jcl-over-slf4j "_"]
                 [org.slf4j/jul-to-slf4j "_"]
                 [org.slf4j/log4j-over-slf4j "_"]
                 [ch.qos.logback/logback-core "_"]
                 [ch.qos.logback/logback-classic "_"
                  :exclusions [org.slf4j/slf4j-api
                               org.slf4j/slf4j-log4j12]]]

  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies [[cambium/cambium.codec-cheshire "_"]
                   [cambium/cambium.logback.json "_"]]}})
