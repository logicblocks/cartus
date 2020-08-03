(ns cartus.test.matchers-test
  (:require
   [clojure.test :refer :all]

   [matcher-combinators.core :as mc-core]
   [matcher-combinators.model :as mc-model]
   [matcher-combinators.standalone :as mc-standalone]

   [cartus.test.matchers :as m]))

(deftest match-candidates-generates-all-candidates-when-n-equals-length
  (is (= #{[1]}
        (m/match-candidates [1] 1)))
  (is (= #{[1 2]
           [::m/any 1] [::m/any 2]
           [1 ::m/any] [2 ::m/any]}
        (m/match-candidates [1 2] 2)))
  (is (= #{[1 2 3]
           [::m/any 1 2] [::m/any 1 3] [::m/any 2 3]
           [1 ::m/any 2] [1 ::m/any 3] [2 ::m/any 3]
           [1 2 ::m/any] [1 3 ::m/any] [2 3 ::m/any]
           [1 ::m/any ::m/any] [::m/any 1 ::m/any] [::m/any ::m/any 1]
           [2 ::m/any ::m/any] [::m/any 2 ::m/any] [::m/any ::m/any 2]
           [3 ::m/any ::m/any] [::m/any 3 ::m/any] [::m/any ::m/any 3]}
        (m/match-candidates [1 2 3] 3)))
  (is (= #{[1 1 2]
           [::m/any 1 1] [::m/any 1 2]
           [1 ::m/any 1] [1 ::m/any 2]
           [1 1 ::m/any] [1 2 ::m/any]
           [1 ::m/any ::m/any] [::m/any 1 ::m/any] [::m/any ::m/any 1]
           [2 ::m/any ::m/any] [::m/any 2 ::m/any] [::m/any ::m/any 2]}
        (m/match-candidates [1 1 2] 3)))
  (is (= #{[1 1 1]
           [::m/any 1 1]
           [1 ::m/any 1]
           [1 1 ::m/any]
           [1 ::m/any ::m/any] [::m/any 1 ::m/any] [::m/any ::m/any 1]}
        (m/match-candidates [1 1 1] 3))))

(deftest match-candidates-generates-candidates-when-n-less-than-length
  (is (= #{[1] [2] [3]}
        (m/match-candidates [1 2 3] 1)))
  (is (= #{[1 2] [1 3] [1 4] [2 3] [2 4] [3 4]
           [::m/any 1] [::m/any 2] [::m/any 3] [::m/any 4]
           [1 ::m/any] [2 ::m/any] [3 ::m/any] [4 ::m/any]}
        (m/match-candidates [1 2 3 4] 2)))
  (is (= #{[1 2 3] [1 2 4] [1 3 4] [2 3 4]
           [1 2 ::m/any] [1 3 ::m/any] [1 4 ::m/any]
           [1 ::m/any 2] [1 ::m/any 3] [1 ::m/any 4]
           [1 ::m/any ::m/any]
           [2 3 ::m/any] [2 4 ::m/any]
           [2 ::m/any 3] [2 ::m/any 4]
           [2 ::m/any ::m/any]
           [3 4 ::m/any]
           [3 ::m/any 4]
           [3 ::m/any ::m/any]
           [4 ::m/any ::m/any]
           [::m/any 1 2] [::m/any 1 3] [::m/any 1 4]
           [::m/any 1 ::m/any]
           [::m/any 2 3] [::m/any 2 4]
           [::m/any 2 ::m/any]
           [::m/any 3 4]
           [::m/any 3 ::m/any]
           [::m/any 4 ::m/any]
           [::m/any ::m/any 1] [::m/any ::m/any 2]
           [::m/any ::m/any 3] [::m/any ::m/any 4]}
        (m/match-candidates [1 2 3 4] 3))))

(deftest subsequences-matches-if-actual-is-supersequence-of-expected
  (is (true? (mc-standalone/match? (m/subsequences [1 2 3]) [1 2 3])))
  (is (true? (mc-standalone/match? (m/subsequences [1 2 3]) [0 1 2 3 4])))
  (is (true? (mc-standalone/match? (m/subsequences '(1 3)) '(0 1 2 3 4)))))

(deftest subsequences-matches-if-actual-is-supersequence-of-expected-matchers
  (is (true? (mc-standalone/match?
               (m/subsequences [odd? even? integer?])
               [1 2 3])))
  (is (true? (mc-standalone/match?
               (m/subsequences [odd? even? 3])
               [0 1 2 3])))
  (is (true? (mc-standalone/match?
               (m/subsequences (list odd? odd?))
               '(0 1 2 3 4)))))

(deftest subsequences-matches-if-actual-and-expected-different-sequential-types
  (is (true? (mc-standalone/match? (m/subsequences '(1 2 3)) [1 2 3])))
  (is (true? (mc-standalone/match? (m/subsequences [1 2 3]) '(0 1 2 3 4))))
  (is (true? (mc-standalone/match? (m/subsequences '(1 3)) [0 1 2 3 4]))))

(deftest subsequences-mismatches-if-actual-is-not-supersequence-of-expected
  (is (false? (mc-standalone/match? (m/subsequences [1 2 3]) [1 3 2])))
  (is (false? (mc-standalone/match? (m/subsequences [1 2 3]) [0 1 2 4])))
  (is (false? (mc-standalone/match? (m/subsequences [1 3]) [0 3 2 1 4])))
  (is (false? (mc-standalone/match? (m/subsequences [1 2 3]) [1 2])))
  (is (false? (mc-standalone/match? (m/subsequences [1 2 3]) [2 3]))))

(deftest subsequences-mismatches-if-actual-does-not-satisfy-matchers
  (is (false? (mc-standalone/match?
                (m/subsequences [#(< 3 %) #(< 3 %) 3])
                [1 3 2]))))

(deftest subsequences-describes-mismatch-when-element-missing
  (let [element-0-ignored (m/->Ignored 0)
        element-4-ignored (m/->Ignored 4)
        element-3-missing (mc-model/->Missing 3)]
    (is (= {:matcher-combinators.result/type   :mismatch
            :matcher-combinators.result/value  [element-0-ignored
                                                1
                                                2
                                                element-4-ignored
                                                element-3-missing]
            :matcher-combinators.result/weight 1}
          (mc-core/match (m/subsequences [1 2 3]) [0 1 2 4])))))

(deftest subsequences-describes-mismatch-when-order-wrong-but-same-elements
  (let [element-3-ignored (m/->Ignored 3)
        element-2-ignored (m/->Ignored 2)
        element-2-missing (mc-model/->Missing 2)
        element-3-missing (mc-model/->Missing 3)]
    (is (= {:matcher-combinators.result/type   :mismatch
            :matcher-combinators.result/value  [element-3-ignored
                                                element-2-ignored
                                                1
                                                element-2-missing
                                                element-3-missing]
            :matcher-combinators.result/weight 2}
          (mc-core/match (m/subsequences [1 2 3]) [3 2 1])))))

(deftest subsequences-mismatches-if-actual-is-missing
  (is (= {:matcher-combinators.result/type   :mismatch
          :matcher-combinators.result/value  (mc-model/->Missing [1 2 3])
          :matcher-combinators.result/weight 1}
        (mc-core/-match (m/subsequences [1 2 3])
          :matcher-combinators.core/missing))))

(deftest subsequences-mismatches-if-expected-is-not-sequential
  (let [mismatch (mc-model/->InvalidMatcherType
                   (str "provided: " {:first 1 :second 2})
                   (str "subsequences should be called with "
                     "'expected' argument of type: seq"))]
    (is (= {:matcher-combinators.result/type   :mismatch
            :matcher-combinators.result/value  mismatch
            :matcher-combinators.result/weight 1}
          (mc-core/-match (m/subsequences {:first 1 :second 2})
            [1 2 3])))))

(deftest subsequences-mismatches-if-actual-is-not-sequential
  (let [mismatch (mc-model/->Mismatch
                   [1 2 3]
                   {:first 1 :second 2})]
    (is (= {:matcher-combinators.result/type   :mismatch
            :matcher-combinators.result/value  mismatch
            :matcher-combinators.result/weight 1}
          (mc-core/-match (m/subsequences [1 2 3])
            {:first 1 :second 2})))))
