(ns cartus.test.matchers
  (:require
   [clojure.tools.trace :as trace]
   [clojure.math.combinatorics :as comb]

   [matcher-combinators.core :as mc]
   [matcher-combinators.model :as mc-model]
   [matcher-combinators.result :as mc-result])
  (:import
   [matcher_combinators.model Missing Mismatch]))

(defrecord Ignored [actual])

(defn- validate-input
  ([expected actual pred matcher-name type]
   (validate-input expected actual pred pred matcher-name type))
  ([expected actual expected-pred actual-pred matcher-name type]
   (cond
     (= actual ::mc/missing)
     {::mc-result/type   :mismatch
      ::mc-result/value  (mc-model/->Missing expected)
      ::mc-result/weight 1}

     (not (expected-pred expected))
     {::mc-result/type   :mismatch
      ::mc-result/value  (mc-model/->InvalidMatcherType
                           (str "provided: " expected)
                           (str matcher-name " "
                             "should be called with "
                             "'expected' argument of type: "
                             type))
      ::mc-result/weight 1}

     (not (actual-pred actual))
     {::mc-result/type   :mismatch
      ::mc-result/value  (mc-model/->Mismatch expected actual)
      ::mc-result/weight 1}

     :else
     nil)))

(defn insert [v i e]
  (vec (concat (take i v) [e] (drop i v))))

(defn match-candidates [elements n]
  (apply concat
    (comb/combinations elements n)
    (for [i (range 1 (inc n))]
      (mapcat
        #(for [ps (comb/combinations (range n) i)]
           (reduce (fn [c p] (insert c p ::mc/missing)) % ps))
        (comb/combinations elements (- n i))))))

(defn match-candidate-result [match-candidate matchers]
  (reduce
    (fn [{::mc-result/keys [type value elements matched weight]}
         [matcher element]]
      (let [{element-value  ::mc-result/value
             element-weight ::mc-result/weight
             :as            element-result}
            (mc/match matcher element)

            type (if (mc/indicates-match? element-result) type :mismatch)
            value (concat value [element-value])
            elements (concat elements [element])
            matched (if (mc/indicates-match? element-result)
                      (concat matched [element])
                      matched)
            weight (+ weight element-weight)]
        #::mc-result{:type     type
                     :value    value
                     :elements elements
                     :matched  matched
                     :weight   weight}))
    #::mc-result{:type     :match
                 :value    (empty matchers)
                 :elements (empty matchers)
                 :matched  (empty matchers)
                 :weight   0}
    (map vector matchers match-candidate)))

(defn format-mismatch [result elements]
  (let [{:keys [value remaining]}
        (reduce
          (fn [{:keys [remaining value]} element]
            (let [[missing [next]]
                  (split-with #(instance? Missing (first %)) remaining)]
              (cond
                (= element (second next))
                {:remaining (drop (inc (count missing)) remaining)
                 :value     (concat value (map first missing)
                              [(first next)])}

                (instance? Mismatch (first next))
                {:remaining (drop (inc (count missing)) remaining)
                 :value     (concat value (map first missing)
                              [(first next)])}

                :else
                {:remaining remaining
                 :value     (concat value [(->Ignored element)])})))
          {:remaining (map vector
                        (::mc-result/value result)
                        (::mc-result/elements result))
           :value     []}
          elements)]
    #::mc-result{:type   :mismatch
                 :value  (concat value (map first remaining))
                 :weight (::mc-result/weight result)}))

(defn- better-mismatch? [best candidate]
  (let [best-matched (-> best ::mc-result/matched count)
        candidate-matched (-> candidate ::mc-result/matched count)
        candidate-weight (::mc-result/weight candidate)
        best-weight (::mc-result/weight best)]
    (and (>= candidate-matched best-matched)
      (< candidate-weight best-weight))))

(defn match-subsequence [matchers elements]
  (let [result
        (reduce
          (fn [best candidate]
            (let [result (match-candidate-result candidate matchers)]
              (cond
                (mc/indicates-match? result) (reduced ::mc/match-found)
                (better-mismatch? best result) result
                :else best)))
          #::mc-result{:type     :mismatch
                       :value    (empty matchers)
                       :elements (empty matchers)
                       :matched  (empty matchers)
                       :weight   Integer/MAX_VALUE}
          (match-candidates elements (count matchers)))]
    (if (= ::mc/match-found result)
      #::mc-result{:type   :match
                   :value  elements
                   :weight 0}
      (format-mismatch result elements))))

(defrecord SubsequencesSeq
  [expected]
  mc/Matcher
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
    :else (mc/->InvalidType expected "subsequences" "seq")))
