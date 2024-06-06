(ns cartus.redaction-test
  (:require
   [clojure.test :refer :all]
   [cartus.test :as log-test :refer [logged?]]))

(deftest with-redaction-test
  (testing "can transform logger to redact keys"))