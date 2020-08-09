(ns cartus.test.matchers
  (:require
   [clojure.tools.trace :as trace]
   [clojure.math.combinatorics :as comb]

   [matcher-combinators.core :as mc-core]
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
     (= actual ::mc-core/missing)
     {::mc-result/type   :mismatch
      ::mc-result/value  (mc-model/->Missing
                           (if (fn? expected)
                             (str "predicate: " expected)
                             expected))
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
        #(for [ps (apply comb/cartesian-product
                    (repeat i (range n)))
               :when (apply < ps)]
           (reduce (fn [c p] (insert c p ::missing)) % ps))
        (comb/combinations elements (- n i))))))

(defn match-candidate-result [match-candidate matchers]
  (trace/trace "match-candidate" match-candidate)
  (let [results
        (map
          (fn [matcher element]
            (trace/trace "matcher" matcher)
            (trace/trace "element" element)
            {:matcher matcher
             :element element
             :result  (if (= ::missing element)
                        {::mc-result/type   :mismatch
                         ::mc-result/value  (mc-model/->Missing
                                              (if (fn? matcher)
                                                (str "predicate: " matcher)
                                                matcher))
                         ::mc-result/weight 1}
                        (mc-core/match matcher element))})
          matchers
          match-candidate)
        result
        (reduce
          (fn [overall-result {:keys [matcher element result]}]
            {::mc-result/type     (if (= (::mc-result/type overall-result)
                                        :mismatch)
                                    :mismatch
                                    (::mc-result/type result))
             ::mc-result/value    (concat
                                    (::mc-result/value overall-result)
                                    [(::mc-result/value result)])
             ::mc-result/matchers (concat
                                    (::mc-result/matchers overall-result)
                                    [matcher])
             ::mc-result/elements (concat
                                    (::mc-result/elements overall-result)
                                    [element])
             ::mc-result/weight   (+
                                    (::mc-result/weight overall-result)
                                    (::mc-result/weight result))})
          {::mc-result/type     :match
           ::mc-result/value    (empty matchers)
           ::mc-result/matchers (empty matchers)
           ::mc-result/elements (empty matchers)
           ::mc-result/weight   0}
          results)]
    result))

(defn match-subsequence [matchers elements]
  (let [match-candidates (match-candidates elements (count matchers))
        match-results
        (map (fn [c]
               (let [r (match-candidate-result c matchers)]
                 (trace/trace "match-result" r)
                 r))
          match-candidates)
        match
        (first
          (filter
            #(= (::mc-result/type %) :match)
            match-results))]
    (trace/trace "match" match)
    (or match
      (let [best-mismatch
            (first (sort-by ::mc-result/weight match-results))
            _ (trace/trace "best-mismatch" best-mismatch)
            {:keys [value remaining]}
            (reduce
              (fn [{:keys [remaining value]} element]
                (let [[missing [next]]
                      (split-with #(instance? Missing (first %)) remaining)]
                  (trace/trace "--------------" nil)
                  (trace/trace "remaining" remaining)
                  (trace/trace "value" value)
                  (trace/trace "element" element)
                  (trace/trace "missing" missing)
                  (trace/trace "next" next)
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
                            (::mc-result/value best-mismatch)
                            (::mc-result/elements best-mismatch))
               :value     []}
              elements)]
        {::mc-result/type   :mismatch
         ::mc-result/value  (concat value (map first remaining))
         ::mc-result/weight (::mc-result/weight best-mismatch)}))))

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
