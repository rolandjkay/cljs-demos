(defproject parabola "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [proto-repl "0.3.1"]
                 [devcards "0.2.1"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.3.0"]
                 [re-frame "0.10.4"]
                 [secretary "1.2.3"]
                 [adzerk/cljs-console "0.1.1"]
                 [cljsjs/interact "1.2.8-0"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]}

  :profiles
  {:dev
   {:repl-options {:init-ns parabola.repl
                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
    :dependencies [[binaryage/devtools "0.9.4"]
                   [org.clojure/test.check "0.9.0"]
                   [lein-doo "0.1.8"]
                   [com.cemerick/piggieback "0.2.2"]]

    :plugins      [[lein-figwheel "0.5.13"]
                   [lein-doo "0.1.8"]]}}

  :doo {:build "test"
        :alias {:default [:phantom]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/cljc"]
     :figwheel     {:on-jsload "parabola.core/mount-root"}
     :compiler     {:main                 parabola.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id "devcards"
       :source-paths ["src/cljs" "test/cljs"]
       :figwheel { :devcards true}
       :compiler { :main       parabola.devcards
                   :asset-path "js/compiled/devcards_out"
                   :output-to  "resources/public/js/compiled/devcards.js"
                   :output-dir "resources/public/js/compiled/devcards_out"
                   :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs" "src/cljc"]
     :compiler     {:main            parabola.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "src/cljc" "test/cljs"]
     :compiler     {:main          parabola.doo-runner
                    :optimizations :whitespace
                    :output-to     "target/testable.js"
                    :output-dir    "target"}}]})
