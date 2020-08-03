(ns cartus.test-support.reports
  (:require
   [clojure.test :as test]

   [matcher-combinators.model :as mc-model]))

;; It should be enough to rebind clojure.test/report to capture reports.
;; However, it appears Cursive redefs clojure.test/do-report (a level above the
;; clojure.test/report macro), so to avoid Cursive swallowing reports, it's
;; necessary to capture the original clojure.test/do-report binding and
;; restore it in report-on

(def do-report test/do-report)

(defmacro report-on [form]
  `(let [reports# (atom nil)]
     (with-redefs [test/do-report do-report]
       (binding [test/report (fn [m#] (swap! reports# conj m#))]
         (test/is ~form)))
     #_(clojure.pprint/pprint (deref reports#))
     #_(println (deref reports#))
     (first (deref reports#))))

(defn missing [expected]
  (mc-model/map->Missing {:expected expected}))

(defn mismatch [{:keys [expected actual]}]
  (mc-model/map->Mismatch
    {:expected expected
     :actual   actual}))
