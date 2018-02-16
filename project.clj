(defproject parabola "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.3.0"]
                 [re-frame "0.10.4"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [org.clojure/test.check "0.9.0"]
                   [lein-doo "0.1.8"]]

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
