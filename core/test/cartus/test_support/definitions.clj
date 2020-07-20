(ns cartus.test-support.definitions
  (:require
   [cartus.core :as cartus]))

(def level-defs
  [{:level-keyword    :trace
    :slf4j-level-name "trace"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/trace logger type context opts))
                       :meta   {:line 9 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/trace logger type context))
                       :meta   {:line 12 :column 34}}}
   {:level-keyword    :debug
    :slf4j-level-name "debug"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/debug logger type context opts))
                       :meta   {:line 17 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/debug logger type context))
                       :meta   {:line 20 :column 34}}}
   {:level-keyword    :info
    :slf4j-level-name "info"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/info logger type context opts))
                       :meta   {:line 25 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/info logger type context))
                       :meta   {:line 28 :column 34}}}
   {:level-keyword    :warn
    :slf4j-level-name "warn"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/warn logger type context opts))
                       :meta   {:line 33 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/warn logger type context))
                       :meta   {:line 36 :column 34}}}
   {:level-keyword    :error
    :slf4j-level-name "error"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/error logger type context opts))
                       :meta   {:line 41 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/error logger type context))
                       :meta   {:line 44 :column 34}}}
   {:level-keyword    :fatal
    :slf4j-level-name "error"
    :with-opts        {:log-fn (fn [logger type context opts]
                                 (cartus/fatal logger type context opts))
                       :meta   {:line 49 :column 34}}
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/fatal logger type context))
                       :meta   {:line 52 :column 34}}}])
