(ns cartus.test-support.definitions
  (:require
   [cartus.core :as cartus]))

(def level-defs
  [{:level-keyword :trace
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/trace logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/trace logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/trace logger type))
                    :meta   {:line 1 :column 1}}}
   {:level-keyword :debug
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/debug logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/debug logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/debug logger type))
                    :meta   {:line 1 :column 1}}}
   {:level-keyword :info
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/info logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/info logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/info logger type))
                    :meta   {:line 1 :column 1}}}
   {:level-keyword :warn
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/warn logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/warn logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/warn logger type))
                    :meta   {:line 1 :column 1}}}
   {:level-keyword :error
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/error logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/error logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/error logger type))
                    :meta   {:line 1 :column 1}}}
   {:level-keyword :fatal
    :arity-4       {:log-fn (fn [logger type context opts]
                              ^{:line 1 :column 1}
                              (cartus/fatal logger type context opts))
                    :meta   {:line 1 :column 1}}
    :arity-3       {:log-fn (fn [logger type context]
                              ^{:line 1 :column 1}
                              (cartus/fatal logger type context))
                    :meta   {:line 1 :column 1}}
    :arity-2       {:log-fn (fn [logger type]
                              ^{:line 1 :column 1}
                              (cartus/fatal logger type))
                    :meta   {:line 1 :column 1}}}])
