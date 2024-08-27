(defproject io.logicblocks/cartus.redaction "0.1.19-RC21"
  :description "A redaction transformer for cartus."

  :plugins  [[lein-modules "0.3.11"]]

  :dependencies [[io.logicblocks/cartus.core :version]]
  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies [[io.logicblocks/cartus.test :version]]}})
