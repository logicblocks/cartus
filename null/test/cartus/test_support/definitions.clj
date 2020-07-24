(ns cartus.test-support.definitions
  (:require
   [cartus.core :as cartus]))

(def level-defs
  [{:level-keyword    :trace
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/trace logger type context))}}
   {:level-keyword    :debug
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/debug logger type context))}}
   {:level-keyword    :info
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/info logger type context))}}
   {:level-keyword    :warn
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/warn logger type context))}}
   {:level-keyword    :error
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/error logger type context))}}
   {:level-keyword    :fatal
    :without-opts     {:log-fn (fn [logger type context]
                                 (cartus/fatal logger type context))}}])
