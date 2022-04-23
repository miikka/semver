(defproject org.clojurs.miikka/semver "0.1.0-SNAPSHOT"
  :description "semver parser for Clojure"
  :url "https://github.com/miikka/semver"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [instaparse/instaparse "1.4.11"]]
  :profiles {:dev {:dependencies [[lambdaisland/kaocha "1.65.1029"]]}}
  :aliases {"kaocha" ["run" "-m" "kaocha.runner"]}
  :repl-options {:init-ns semver.core})
