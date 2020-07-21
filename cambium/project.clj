(defproject io.logicblocks/cartus.cambium "0.1.7-RC1"
  :description "A structured logging abstraction with multiple backends."

  :plugins  [[lein-modules "0.3.11"]
             [lein-cloverage "1.1.2"]
             [lein-shell "0.5.0"]
             [lein-cprint "1.3.3"]
             [lein-ancient "0.6.15"]
             [lein-eftest "0.5.9"]
             [lein-cljfmt "0.6.7"]
             [lein-kibit "0.1.8"]
             [lein-bikeshed "0.5.2"]
             [jonase/eastwood "0.3.11"]]

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
