(ns parabola.components-cards
  (:require [cljsjs.interact]
            [reagent.core :as reagent]
            [sablono.core :as sab] ; just for example
            [parabola.objects :refer [object->svg]]
            [parabola.domain :as d]
            [clojure.spec.alpha :as s])
  (:require-macros
            [devcards.core :refer [defcard]]))

(defn my-component
  []
  (let [positions (reagent/atom {:football [10 10] :beachball [190 190]})      ;; <-- closed over by lifecycle fns
        target (reagent/atom "None")]
     (reagent/create-class                 ;; <-- expects a map of functions
       {:component-did-mount               ;; the name of a lifecycle function
         #(-> ".drag-me"
              js/interact
              (.draggable true)
              (.on "dragmove"
                (fn [e]
                  (let [target-id (-> e .-target .-id)]
                    (swap! positions update (keyword target-id) (partial mapv + [(.-dx e) (.-dy e)]))
                    (reset! target target-id)))))

        :component-will-mount              ;; the name of a lifecycle function
        #(println "component-will-mount")  ;; your implementation

        ;; other lifecycle funcs can go in here

        :display-name  "my-component"  ;; for more helpful warnings & errors

        :reagent-render        ;; Note:  is not :render
         (fn []           ;; remember to repeat parameters
           [:div
            [:p "Interacting with " @target]
            [:svg {:width "200" :height "200"}
              [:circle.drag-me
                {:id "football"
                 :cx (get-in @positions [:football 0])
                 :cy (get-in @positions [:football  1])
                 :r "3" :stroke "black" :fill "black"}]
              [:circle.drag-me
                {:id "beachball"
                 :cx (get-in @positions [:beachball 0])
                 :cy (get-in @positions [:beachball 1])
                 :r "5" :stroke "black" :fill "yellow"}]]])})))



(defcard linear-path
  (reagent/as-element
    [:div
      [:h1 "Drag test"]
      "Drag the circle around to test " [:a {:href "http://interactjs.io"} "interactjs"]
      [my-component]]))
