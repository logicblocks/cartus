(ns cartus.test.matchers-test
  (:require
   [clojure.test :refer :all]

   [matcher-combinators.core :as mc-core]
   [matcher-combinators.model :as mc-model]
   [matcher-combinators.matchers :as mc-matchers]
   [matcher-combinators.result :as mc-result]
   [matcher-combinators.standalone :as mc-standalone]
   [matcher-combinators.parser]

   [cartus.test.matchers :as m]))

(deftest match-candidates-generates-all-candidates-when-n-equals-length
  (is (= [[1]
          [::mc-core/missing]]
        (m/match-candidates [1] 1)))
  (is (= [[1 2]
          [::mc-core/missing 1] [1 ::mc-core/missing]
          [::mc-core/missing 2] [2 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing]]
        (m/match-candidates [1 2] 2)))
  (is (= [[1 2 3]
          [::mc-core/missing 1 2]
          [1 ::mc-core/missing 2]
          [1 2 ::mc-core/missing]
          [::mc-core/missing 1 3]
          [1 ::mc-core/missing 3]
          [1 3 ::mc-core/missing]
          [::mc-core/missing 2 3]
          [2 ::mc-core/missing 3]
          [2 3 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 1]
          [::mc-core/missing 1 ::mc-core/missing]
          [1 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 2]
          [::mc-core/missing 2 ::mc-core/missing]
          [2 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 3]
          [::mc-core/missing 3 ::mc-core/missing]
          [3 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing ::mc-core/missing]]
        (m/match-candidates [1 2 3] 3)))
  (is (= [[1 1 2]
          [::mc-core/missing 1 1]
          [1 ::mc-core/missing 1]
          [1 1 ::mc-core/missing]
          [::mc-core/missing 1 2]
          [1 ::mc-core/missing 2]
          [1 2 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 1]
          [::mc-core/missing 1 ::mc-core/missing]
          [1 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 2]
          [::mc-core/missing 2 ::mc-core/missing]
          [2 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing ::mc-core/missing]]
        (m/match-candidates [1 1 2] 3)))
  (is (= [[1 1 1]
          [::mc-core/missing 1 1]
          [1 ::mc-core/missing 1]
          [1 1 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 1]
          [::mc-core/missing 1 ::mc-core/missing]
          [1 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing ::mc-core/missing]]
        (m/match-candidates [1 1 1] 3))))

(deftest match-candidates-generates-candidates-when-n-less-than-length
  (is (= [[1] [2] [3] [::mc-core/missing]]
        (m/match-candidates [1 2 3] 1)))
  (is (= [[1 2] [1 3] [1 4] [2 3] [2 4] [3 4]
          [::mc-core/missing 1] [1 ::mc-core/missing]
          [::mc-core/missing 2] [2 ::mc-core/missing]
          [::mc-core/missing 3] [3 ::mc-core/missing]
          [::mc-core/missing 4] [4 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing]]
        (m/match-candidates [1 2 3 4] 2)))
  (is (= [[1 2 3] [1 2 4] [1 3 4] [2 3 4]
          [::mc-core/missing 1 2]
          [1 ::mc-core/missing 2]
          [1 2 ::mc-core/missing]
          [::mc-core/missing 1 3]
          [1 ::mc-core/missing 3]
          [1 3 ::mc-core/missing]
          [::mc-core/missing 1 4]
          [1 ::mc-core/missing 4]
          [1 4 ::mc-core/missing]
          [::mc-core/missing 2 3]
          [2 ::mc-core/missing 3]
          [2 3 ::mc-core/missing]
          [::mc-core/missing 2 4]
          [2 ::mc-core/missing 4]
          [2 4 ::mc-core/missing]
          [::mc-core/missing 3 4]
          [3 ::mc-core/missing 4]
          [3 4 ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 1]
          [::mc-core/missing 1 ::mc-core/missing]
          [1 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 2]
          [::mc-core/missing 2 ::mc-core/missing]
          [2 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 3]
          [::mc-core/missing 3 ::mc-core/missing]
          [3 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing 4]
          [::mc-core/missing 4 ::mc-core/missing]
          [4 ::mc-core/missing ::mc-core/missing]
          [::mc-core/missing ::mc-core/missing ::mc-core/missing]]
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

(deftest subsequences-matches-for-nested-elements
  (is (true? (mc-standalone/match?
               (m/subsequences
                 [{:first "a" :second "b"}
                  {:first "c" :second "d"}])
               [{:first "x" :second "y" :third 1}
                {:first "a" :second "b" :third 2}
                {:first "c" :second "d" :third 3}
                {:first "u" :second "v" :third 4}])))
  (is (true? (mc-standalone/match?
               (m/subsequences
                 [[1 2] [3 4]])
               [[8 9] [1 2] [10 11] [3 4] [12 13]]))))

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

(deftest subsequences-mismatches-if-actual-is-empty
  (is (false? (mc-standalone/match? (m/subsequences [1]) [])))
  (is (false? (mc-standalone/match? (m/subsequences [even?]) []))))

(deftest subsequences-mismatches-if-actual-does-not-satisfy-matchers
  (is (false? (mc-standalone/match?
                (m/subsequences [#(< 3 %) #(< 3 %) 3])
                [1 3 2]))))

(deftest subsequences-mismatches-for-nested-elements
  (is (false? (mc-standalone/match?
                (m/subsequences
                  [{:first "l" :second "m"}
                   {:first "c" :second "d"}])
                [{:first "x" :second "y" :third 1}
                 {:first "a" :second "b" :third 2}
                 {:first "c" :second "d" :third 3}
                 {:first "u" :second "v" :third 4}])))
  (is (false? (mc-standalone/match?
                (m/subsequences
                  [[0 1] [3 4]])
                [[8 9] [1 2] [10 11] [3 4] [12 13]]))))

(deftest subsequences-describes-mismatch-when-order-wrong-but-same-elements
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [(mc-model/map->Mismatch
                                {:actual 3 :expected 1})
                              2
                              (mc-model/map->Mismatch
                                {:actual 1 :expected 3})]
          ::mc-result/weight 2}
        (mc-core/match (m/subsequences [1 2 3]) [3 2 1]))))

(deftest subsequences-describes-mismatch-when-element-missing
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [(m/->Ignored 0)
                              1
                              2
                              (mc-model/map->Mismatch
                                {:actual 4 :expected 3})]
          ::mc-result/weight 1}
        (mc-core/match (m/subsequences [1 2 3]) [0 1 2 4]))))

(deftest subsequences-describes-mismatch-when-order-wrong-and-less-elements
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [(mc-model/map->Mismatch
                                {:actual 0 :expected 1})
                              3
                              (m/->Ignored 2)
                              (m/->Ignored 1)
                              (m/->Ignored 4)]
          ::mc-result/weight 1}
        (mc-core/match (m/subsequences [1 3]) [0 3 2 1 4]))))

(deftest subsequences-describes-mismatch-when-expected-more-elements
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [1
                              2
                              (mc-model/->Missing 3)]
          ::mc-result/weight 1}
        (mc-core/match (m/subsequences [1 2 3]) [1 2]))))

(deftest subsequences-describes-mismatch-when-actual-is-empty
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [(mc-model/->Missing {:first 1})
                              (mc-model/->Missing {:second 2})
                              (mc-model/->Missing {:third 3})]
          ::mc-result/weight 3}
        (mc-core/match
         (m/subsequences [{:first 1} {:second 2} {:third 3}])
          []))))

(deftest subsequences-describes-mismatch-when-predicates-not-satisfied
  (let [greater-than-3 (mc-matchers/pred (fn [x] (< 3 x)))]
    (is (= {::mc-result/type   :mismatch
            ::mc-result/value
            [(mc-model/->Missing (:desc greater-than-3))
             (mc-model/map->Mismatch
               {:actual   1
                :expected (:desc greater-than-3)})
             3
             (m/->Ignored 2)]
            ::mc-result/weight 2}
          (mc-core/match
           (m/subsequences [greater-than-3 greater-than-3 3])
            [1 3 2])))))

(deftest subsequences-describes-mismatch-for-nested-elements
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  [(m/->Ignored
                                {:first "x" :second "y" :third 1})
                              {:first  (mc-model/map->Mismatch
                                         {:actual   "a"
                                          :expected "l"})
                               :second "b"
                               :third  2}
                              {:first "c" :second "d" :third 3}
                              (m/->Ignored
                                {:first "u" :second "v" :third 4})]
          ::mc-result/weight 1}
        (mc-core/match
         (m/subsequences
           [{:first "l" :second "b"}
            {:first "c" :second "d"}])
          [{:first "x" :second "y" :third 1}
           {:first "a" :second "b" :third 2}
           {:first "c" :second "d" :third 3}
           {:first "u" :second "v" :third 4}]))))

(deftest subsequences-mismatches-if-actual-is-missing
  (is (= {::mc-result/type   :mismatch
          ::mc-result/value  (mc-model/->Missing [1 2 3])
          ::mc-result/weight 1}
        (mc-core/-match (m/subsequences [1 2 3])
          :matcher-combinators.core/missing))))

(deftest subsequences-mismatches-if-expected-is-not-sequential
  (let [mismatch (mc-model/->InvalidMatcherType
                   (str "provided: " {:first 1 :second 2})
                   (str "subsequences should be called with "
                     "'expected' argument of type: seq"))]
    (is (= {::mc-result/type   :mismatch
            ::mc-result/value  mismatch
            ::mc-result/weight 1}
          (mc-core/-match (m/subsequences {:first 1 :second 2})
            [1 2 3])))))

(deftest subsequences-mismatches-if-actual-is-not-sequential
  (let [mismatch (mc-model/->Mismatch
                   [1 2 3]
                   {:first 1 :second 2})]
    (is (= {::mc-result/type   :mismatch
            ::mc-result/value  mismatch
            ::mc-result/weight 1}
          (mc-core/-match (m/subsequences [1 2 3])
            {:first 1 :second 2})))))
