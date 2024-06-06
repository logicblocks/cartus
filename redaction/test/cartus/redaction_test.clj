(ns cartus.redaction-test
  (:require
   [clojure.test :refer :all]
   [clojure.string :as str]
   [cartus.redaction :as sut]
   [cartus.core :as log]
   [cartus.test :as test-log :refer [logged?]]))

(deftest with-redaction-test
  (testing "can transform logger to redact sensitive keys"
    (let [logger (test-log/logger)
          redacted-logger (sut/with-redaction logger)]
      (log/info
        redacted-logger
        ::something-happened
        {:data          {:token        "secret-token"
                         :secret-token "super-secret-token"}
         :passwords     [{:password "password1"}
                         {:password "password2"}]
         :secret        "birds aren't real"
         :authorization "authorization-deets"
         :secret-key    "G flat"
         :other         "some other value"})
      (is (logged?
            logger
            #{:once}
            {:type    ::something-happened
             :context {:data          {:token        "[REDACTED]"
                                       :secret-token "[REDACTED]"}
                       :passwords     [{:password "[REDACTED]"}
                                       {:password "[REDACTED]"}]
                       :secret        "[REDACTED]"
                       :authorization "[REDACTED]"
                       :secret-key    "[REDACTED]"
                       :other         "some other value"}}))))

  (testing "can override function for determining how/when to redact"
    (let [logger (test-log/logger)
          options {:redact-value-fn (fn [_k v]
                                      (when (-> v
                                              str/lower-case
                                              (str/includes? "voldemort"))
                                        "He who must not be named"))}
          redacted-logger (sut/with-redaction logger options)]
      (log/info redacted-logger ::a-log {:character "Harry Potter"})
      (log/info redacted-logger ::a-log {:character "Hagrid"})
      (log/info redacted-logger ::a-log {:character "Lord Voldemort"})
      (log/info redacted-logger ::a-log {:character "Hermione Granger"})
      (is (logged?
            logger
            #{:once}
            {:context {:character "Harry Potter"}}
            {:context {:character "Hagrid"}}
            {:context {:character "He who must not be named"}}
            {:context {:character "Hermione Granger"}})))))
