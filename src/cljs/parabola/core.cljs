(ns parabola.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [parabola.events :as events]
            [parabola.routes :as routes]
            [parabola.views :as views]
            [parabola.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  ;(enable-console-print!)
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
