(ns parabola.views
  (:require [re-frame.core :as re-frame]
            [reagent.format :as format]
            [parabola.subs :as subs]
            [parabola.components :as components :refer [toolbar svg-canvas]]))



;; home


(defn home-panel []
  [:div
    [toolbar]
    [svg-canvas]]) ;{:width 600 :height 600}]])

;; about

(defn about-panel []
  [:div "This is the About Page."]
  [:div [:a {:href "#/"} "go to Home Page"]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
