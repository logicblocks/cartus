(defproject io.logicblocks/cartus "0.1.15-RC4"
  :description "Parent for all cartus modules."

  :plugins [[lein-modules "0.3.11"]
            [lein-changelog "0.3.2"]
            [lein-codox "0.10.7"]]

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

    :plugins [[lein-cloverage "1.1.2"]
              [lein-shell "0.5.0"]
              [lein-cprint "1.3.3"]
              [lein-ancient "0.6.15"]
              [lein-eftest "0.5.9"]
              [lein-cljfmt "0.6.7"]
              [lein-kibit "0.1.8"]
              [lein-bikeshed "0.5.2"]
              [jonase/eastwood "0.3.11"]]

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
   {org.clojure/clojure            "1.10.1"
    org.clojure/tools.trace        "0.7.10"

    cambium/cambium.core           "0.9.3"
    cambium/cambium.codec-cheshire "0.9.3"
    cambium/cambium.logback.core   "0.4.3"
    cambium/cambium.logback.json   "0.4.3"

    org.slf4j                      "1.7.30"
    ch.qos.logback                 "1.2.3"

    nrepl                          "0.7.0"

    eftest                         "0.5.9"
    tortue/spy                     "2.0.0"

    nubank/matcher-combinators     "3.1.1"

    org.clojure/math.combinatorics "0.1.6"

    io.logicblocks/cartus.core     :version}}

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
