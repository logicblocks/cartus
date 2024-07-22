(ns cartus.cambium.transformers-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [cartus.cambium.transformers :refer [key-order-transformer]]))

(defn json-key-ordering [json]
  (->> json
    (re-seq #"\"([a-zA-Z0-9\-]*)\"\:")
    (map second)))

(json-key-ordering (json/generate-string {:keyB 2 :keyA 1}))

(deftest key-order-transformer-test
  (testing "can order map keys with ordering"
    (let [keys-in-order ["a" "b" "c" "d" "e"]
          transformer (key-order-transformer keys-in-order)
          m {"e" 1 "c" 1 "a" 1 "d" 1 "b" 1}]
      (is (=
            keys-in-order
            (-> m
              transformer
              (json/generate-string)
              json-key-ordering)))))
  (testing "can partially specify order of keys"
    (let [key-order ["a"]
          transformer (key-order-transformer key-order)
          m {"b" 1 "a" 1}]
      (is (= ["a" "b"]
            (-> m
              transformer
              (json/generate-string)
              json-key-ordering))))))
