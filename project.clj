(defproject io.logicblocks/cartus "0.1.1-RC1"
  :description "A structured logging abstraction with multiple backends."
  :url "https://github.com/logicblocks/cartus"

  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[cambium/cambium.core "0.9.3"]]

  :plugins [[lein-cloverage "1.1.2"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]
            [lein-eftest "0.5.9"]
            [lein-codox "0.10.7"]
            [lein-cljfmt "0.6.7"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]
            [jonase/eastwood "0.3.11"]]

  :profiles
  {:shared
   {:dependencies [[org.clojure/clojure "1.10.1"]
                   [org.clojure/tools.trace "0.7.10"]

                   [nrepl "0.7.0"]

                   [cambium/cambium.codec-cheshire "0.9.3"]
                   [cambium/cambium.logback.core "0.4.3"]
                   [cambium/cambium.logback.json "0.4.3"]

                   [org.slf4j/slf4j-api "1.7.30"]
                   [org.slf4j/jcl-over-slf4j "1.7.30"]
                   [org.slf4j/jul-to-slf4j "1.7.30"]
                   [org.slf4j/log4j-over-slf4j "1.7.30"]
                   [ch.qos.logback/logback-classic "1.2.3"
                    :exclusions [org.slf4j/slf4j-api
                                 org.slf4j/slf4j-log4j12]]

                   [eftest "0.5.9"]]}
   :dev
   [:shared {:source-paths ["dev"]
             :eftest       {:multithread? false}}]
   :test
   [:shared {:eftest {:multithread? false}}]
   :prerelease
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]]}
   :release
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["codox"]
     ["changelog" "release"]
     ["shell" "sed" "-E" "-i.bak" "s/\"[0-9]+\\.[0-9]+\\.[0-9]+\"/\"${:version}\"/g" "README.md"]
     ["shell" "rm" "-f" "README.md.bak"]
     ["shell" "git" "add" "."]
     ["vcs" "commit" "Release version %s [skip ci]"]
     ["vcs" "tag"]
     ["deploy"]
     ["change" "version" "leiningen.release/bump-version" "patch"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["vcs" "push"]]}}

  :cloverage
  {:ns-exclude-regex [#"^user"]}

  :codox
  {:namespaces  [#"^cartus\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/logicblocks/cartus/blob/{version}/{filepath}#L{line}"}

  :bikeshed {:name-collisions false}

  :cljfmt {:indents {#".*"     [[:inner 0]]
                     defrecord [[:block 1] [:inner 1]]}}

  :eastwood {:config-files ["config/linter.clj"]}

  :deploy-repositories
  {"releases"  {:url "https://repo.clojars.org" :creds :gpg}
   "snapshots" {:url "https://repo.clojars.org" :creds :gpg}})
