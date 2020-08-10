(defproject io.logicblocks/cartus.test "0.1.13-RC5"
  :description "A test backend for cartus."

  :plugins  [[lein-modules "0.3.11"]]

  :dependencies [[org.clojure/math.combinatorics "_"]

                 [nubank/matcher-combinators "_"]

                 [io.logicblocks/cartus.core :version]])
