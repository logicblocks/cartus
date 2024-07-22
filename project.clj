(defproject io.logicblocks/cartus "0.1.19-RC12"
  :description "Parent for all cartus modules."

  :plugins [[lein-modules "0.3.11"]
            [lein-changelog "0.3.2"]
            [lein-codox "0.10.8"]]

  :modules
  {:subprocess
   nil

   :inherited
   {:url
    "https://github.com/logicblocks/cartus"

    :license
    {:name "The MIT License"
     :url  "https://opensource.org/licenses/MIT"}

    :deploy-repositories
    {"releases"  {:url "https://repo.clojars.org" :creds :gpg}
     "snapshots" {:url "https://repo.clojars.org" :creds :gpg}}

    :plugins [[lein-cloverage "1.2.4"]
              [lein-shell "0.5.0"]
              [lein-cprint "1.3.3"]
              [lein-ancient "0.7.0"]
              [lein-eftest "0.6.0"]
              [lein-cljfmt "0.9.2"]
              [lein-kibit "0.1.8"]
              [lein-bikeshed "0.5.2"]
              [jonase/eastwood "1.4.0"]]

    :cloverage
    {:ns-exclude-regex [#"^user"]}

    :bikeshed
    {:name-collisions false
     :long-lines      false}

    :cljfmt
    {:indents {#".*"     [[:inner 0]]
               defrecord [[:block 1] [:inner 1]]
               deftype   [[:block 1] [:inner 1]]}}

    :eastwood
    {:config-files
     [~(str (System/getProperty "user.dir") "/config/linter.clj")]}}

   :versions
   {org.clojure/clojure              "1.11.3"
    org.clojure/tools.trace          "0.8.0"
    org.clojure/tools.logging        "1.2.4"

    cambium/cambium.core             "1.1.1"
    cambium/cambium.codec-cheshire   "1.0.0"
    cambium/cambium.logback.core     "0.4.6"
    cambium/cambium.logback.json     "0.4.6"

    com.fasterxml.jackson.core       "2.17.1"
    com.fasterxml.jackson.dataformat "2.17.1"

    org.slf4j                        "2.0.13"
    ch.qos.logback                   "1.5.6"

    nrepl                            "1.1.2"

    eftest                           "0.6.0"
    tortue/spy                       "2.15.0"

    nubank/matcher-combinators       "3.9.1"

    org.clojure/math.combinatorics   "0.3.0"

    io.logicblocks/cartus.core       :version}}

  :profiles
  {:shared
   ^{:pom-scope :test}
   {:dependencies [[org.clojure/clojure "_"]
                   [org.clojure/tools.trace "_"]
                   [nrepl "_"]
                   [eftest "_"]
                   [tortue/spy "_"]]}

   :dev
   [:shared
    {:source-paths ["dev"]
     :eftest       {:multithread? false}}]

   :test
   [:shared
    {:eftest {:multithread? false}}]

   :codox
   [:shared
    {:dependencies [[io.logicblocks/cartus.core :version]

                    [org.clojure/math.combinatorics "_"]

                    [nubank/matcher-combinators "_"]

                    [cambium/cambium.core "_"]
                    [cambium/cambium.codec-cheshire "_"]
                    [cambium/cambium.logback.core "_"]
                    [cambium/cambium.logback.json "_"]

                    [org.slf4j/slf4j-api "_"]
                    [org.slf4j/jcl-over-slf4j "_"]
                    [org.slf4j/jul-to-slf4j "_"]
                    [org.slf4j/log4j-over-slf4j "_"]
                    [ch.qos.logback/logback-classic "_"
                     :exclusions [org.slf4j/slf4j-api
                                  org.slf4j/slf4j-log4j12]]]
     :source-paths ["core/src" "cambium/src" "test/src" "null/src"]}]

   :prerelease
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["modules" "change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["modules" "change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["modules" "deploy"]]}

   :release
   {:release-tasks
    [["shell" "git" "diff" "--exit-code"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["modules" "change" "version" "leiningen.release/bump-version" "release"]
     ["modules" "install"]
     ["changelog" "release"]
     ["shell" "sed" "-E" "-i.bak" "s/cartus\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/cartus.\\\\1 \"${:version}\"/g" "README.md"]
     ["shell" "rm" "-f" "README.md.bak"]
     ["shell" "sed" "-E" "-i.bak" "s/cartus\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/cartus.\\\\1 \"${:version}\"/g" "docs/getting-started.md"]
     ["shell" "rm" "-f" "docs/getting-started.md.bak"]
     ["codox"]
     ["shell" "git" "add" "."]
     ["vcs" "commit" "Release version %s [skip ci]"]
     ["vcs" "tag"]
     ["modules" "deploy"]
     ["change" "version" "leiningen.release/bump-version" "patch"]
     ["modules" "change" "version" "leiningen.release/bump-version" "patch"]
     ["change" "version" "leiningen.release/bump-version" "rc"]
     ["modules" "change" "version" "leiningen.release/bump-version" "rc"]
     ["change" "version" "leiningen.release/bump-version" "release"]
     ["modules" "change" "version" "leiningen.release/bump-version" "release"]
     ["vcs" "commit" "Pre-release version %s [skip ci]"]
     ["vcs" "tag"]
     ["vcs" "push"]]}}

  :source-paths []
  :test-paths []

  :codox
  {:namespaces  [#"^cartus\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/logicblocks/cartus/blob/{version}/{filepath}#L{line}"}

  :aliases {"eastwood" ["modules" "eastwood"]
            "cljfmt"   ["modules" "cljfmt"]
            "kibit"    ["modules" "kibit"]
            "check"    ["modules" "check"]
            "bikeshed" ["modules" "bikeshed"]
            "eftest"   ["modules" "eftest"]
            "temp"     ["shell" "sed" "-E" "-i.bak" "s/cartus\\.(.+) \"[0-9]+\\.[0-9]+\\.[0-9]+\"/cartus.\\\\1 \"${:version}\"/g" "README.md"]})
