(ns cartus.test-support.reports
  (:require
   [clojure.test :as test]

   [matcher-combinators.model :as mc-model]

   [cartus.test.matchers :as cartus-matchers]))

;;; Report hijacking

;; It should be enough to rebind clojure.test/report to capture reports.
;; However, it appears Cursive redefs clojure.test/do-report (a level above the
;; clojure.test/report macro), so to avoid Cursive swallowing reports, it's
;; necessary to capture the original clojure.test/do-report binding and
;; restore it in report-on

(def do-report test/do-report)

(defmacro report-on [form]
  `(let [report# (atom nil)]
     (with-redefs [test/do-report do-report]
       (binding [test/report (fn [m#] (reset! report# m#))]
         (test/is ~form)))
     (deref report#)))

;;; Report helpers

(defn ignored [actual]
  (cartus-matchers/map->Ignored {:actual actual}))

(defn missing [expected]
  (mc-model/map->Missing {:expected expected}))

(defn mismatch [{:keys [expected actual]}]
  (mc-model/map->Mismatch
    {:expected expected
     :actual   actual}))

(defn unexpected [actual]
  (mc-model/map->Unexpected {:actual actual}))
