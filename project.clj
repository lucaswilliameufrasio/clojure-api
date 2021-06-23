(defproject rest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ; Compojure - A basic routing library
                 [compojure "1.6.1"]
                 ; Our Http library for client/server
                 [http-kit "2.5.3"]
                 ; Ring defaults - for query params etc
                 [ring/ring-defaults "0.3.2"]
                 ; Clojure data.JSON library
                 [org.clojure/data.json "2.2.0"]
                 [com.novemberain/monger "3.1.0"]
                 [cheshire "5.10.0"]
                 [ring/ring-json "0.5.1"]
                 [ring "1.9.2"]]
  :main ^:skip-aot rest.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
