(defproject io.logicblocks/cartus.cambium "0.1.13-RC2"
  :description "A cambium backend for cartus."

  :plugins  [[lein-modules "0.3.11"]]

  :dependencies [[io.logicblocks/cartus.core :version]

                 [cambium/cambium.core "_"]
                 [cambium/cambium.logback.core "_"]

                 [org.slf4j/slf4j-api "_"]
                 [org.slf4j/jcl-over-slf4j "_"]
                 [org.slf4j/jul-to-slf4j "_"]
                 [org.slf4j/log4j-over-slf4j "_"]
                 [ch.qos.logback/logback-classic "_"
                  :exclusions [org.slf4j/slf4j-api
                               org.slf4j/slf4j-log4j12]]]

  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies [[cambium/cambium.codec-cheshire "_"]
                   [cambium/cambium.logback.json "_"]]}})
