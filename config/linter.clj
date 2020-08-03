(disable-warning
  {:linter :constant-test
   :for-macro 'clojure.core/map?
   :if-inside-macroexpansion-of #{'clojure.test/is}
   :within-depth 10})

(disable-warning
  {:linter :constant-test
   :for-macro 'clojure.core/set?
   :if-inside-macroexpansion-of #{'clojure.test/is}
   :within-depth 10})
