(ns cartus.null-test
  (:require
   [clojure.test :refer :all]

   [cartus.test-support.definitions :as defs]

   [cartus.null :as cartus-null]))

(deftest logs-to-null-logger-at-level-without-failure
  (doseq [{:keys [without-opts]} defs/level-defs]
    (let [{:keys [log-fn]} without-opts
          logger (cartus-null/logger)
          type ::some.event
          context {:some "context"}

          result (atom nil)]
      (try
        (log-fn logger type context)
        (reset! result :success)
        (catch Throwable t
          (reset! result t)))

      (is (= :success @result)))))
