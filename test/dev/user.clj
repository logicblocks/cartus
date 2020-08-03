(ns user
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.tools.namespace.repl :refer [refresh refresh-all clear]]

   [matcher-combinators.ansi-color :as mc-ansi]

   [eftest.runner :refer [find-tests run-tests]]))

(alter-var-root #'mc-ansi/*use-color*
  (constantly false))

(defn run-tests-in [& dirs]
  (run-tests
    (find-tests dirs)
    {:multithread? false
     :capture-output? false}))
