(defproject org.clojars.cantido/clj-mmap "2.1.0"
  :description "A wrapper over java.nio's mmap() implementation to ease use, and enable mmap'ing files larger than 2GB."
  :url "https://github.com/Cantido/clj-mmap"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]]
                   :resource-paths ["test-resources"]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0-beta4"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})
