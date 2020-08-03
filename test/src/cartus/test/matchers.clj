(ns cartus.test.matchers
  (:require
   [clojure.pprint :as pp]
   [clojure.math.combinatorics :as comb]

   [matcher-combinators.core :as mc-core]
   [matcher-combinators.model :as mc-model])
  (:import
   [matcher_combinators.model Missing Mismatch]
   [java.io StringWriter]))

(defrecord Ignored [actual])

(defn- validate-input
  ([expected actual pred matcher-name type]
   (validate-input expected actual pred pred matcher-name type))
  ([expected actual expected-pred actual-pred matcher-name type]
   (cond
     (= actual :matcher-combinators.core/missing)
     {:matcher-combinators.result/type   :mismatch
      :matcher-combinators.result/value  (mc-model/->Missing expected)
      :matcher-combinators.result/weight 1}

     (not (expected-pred expected))
     {:matcher-combinators.result/type   :mismatch
      :matcher-combinators.result/value  (mc-model/->InvalidMatcherType
                                           (str "provided: " expected)
                                           (str matcher-name " "
                                             "should be called with "
                                             "'expected' argument of type: "
                                             type))
      :matcher-combinators.result/weight 1}

     (not (actual-pred actual))
     {:matcher-combinators.result/type   :mismatch
      :matcher-combinators.result/value  (mc-model/->Mismatch expected actual)
      :matcher-combinators.result/weight 1}

     :else
     nil)))

(defn insert [v i e]
  (vec (concat (take i v) [e] (drop i v))))

(defn- trace [description object]
  (let [writer (new StringWriter)]
    (pp/pprint object writer)
    (print (str (name description) "\n" (str writer)))))

(defn match-candidates [elements n]
  (set
    (apply concat
      (comb/combinations elements n)
      (for [i (range 1 n)]
        (mapcat
          #(for [ps (apply comb/cartesian-product
                      (repeat (- n (- n i)) (range n)))
                 :when (apply < ps)]
             (reduce (fn [c p] (insert c p ::any)) % ps))
          (comb/combinations elements (- n i)))))))

(defn match-candidate-result [match-candidate matchers]
  (let [results
        (map
          (fn [matcher element]
            (if (= ::any element)
              {:matcher-combinators.result/type   :mismatch
               :matcher-combinators.result/value  (mc-model/->Missing matcher)
               :matcher-combinators.result/weight 1}
              (mc-core/match matcher element)))
          matchers
          match-candidate)
        result
        (reduce
          (fn [overall-result element-result]
            (let [type
                  (if (= (:matcher-combinators.result/type overall-result)
                        :mismatch)
                    :mismatch
                    (:matcher-combinators.result/type element-result))
                  value
                  (concat
                    (:matcher-combinators.result/value overall-result)
                    [(:matcher-combinators.result/value element-result)])
                  weight
                  (+
                    (:matcher-combinators.result/weight overall-result)
                    (:matcher-combinators.result/weight element-result))]
              {:matcher-combinators.result/type   type
               :matcher-combinators.result/value  value
               :matcher-combinators.result/weight weight}))
          {:matcher-combinators.result/type   :match
           :matcher-combinators.result/value  (empty matchers)
           :matcher-combinators.result/weight 0}
          results)]
    result))

(defn match-subsequence [matchers elements]
  (let [match-candidates (match-candidates elements (count matchers))
        match-results
        (map #(match-candidate-result % matchers) match-candidates)
        match
        (first
          (filter
            #(= (:matcher-combinators.result/type %) :match)
            match-results))]
    (or match
      (let [best-mismatch
            (first (sort-by :matcher-combinators.result/weight match-results))
            {:keys [value remaining]}
            (reduce
              (fn [{:keys [remaining value]} element]
                (let [[mismatches [next]]
                      (split-with
                        (fn [e]
                          (or
                            (instance? Missing e)
                            (instance? Mismatch e)))
                        remaining)]
                  (if (= element next)
                    {:remaining (drop (inc (count mismatches)) remaining)
                     :value     (concat value mismatches [element])}
                    {:remaining remaining
                     :value     (concat value [(->Ignored element)])})))
              {:remaining (:matcher-combinators.result/value best-mismatch)
               :value     []}
              elements)
            remaining
            (map (fn [e] (if (instance? Mismatch e)
                           (mc-model/->Missing (:expected e))
                           e))
              remaining)]
        (assoc best-mismatch
          :matcher-combinators.result/value (concat value remaining))))))

(defrecord SubsequencesSeq [expected]
           mc-core/Matcher
           (-matcher-for [this] this)
           (-matcher-for [this _] this)
           (-match [_ actual]
             (if-let [issue
                      (validate-input
                        expected actual sequential? 'subsequences "sequential")]
               issue
               (match-subsequence expected actual))))

(defn subsequences
  "Matcher that will match when a sequence contains each of the expected items
  in the correct order, allowing for surplus items and gaps between items."
  [expected]
  (cond
    (sequential? expected) (->SubsequencesSeq expected)
    :else (mc-core/->InvalidType expected "subsequences" "seq")))
