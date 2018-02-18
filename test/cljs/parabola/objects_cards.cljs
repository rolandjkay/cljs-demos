(ns parabola.objects-cards
  (:require
   [sablono.core :as sab] ; just for example
   [parabola.objects :refer [object->svg]]
   [parabola.domain :as d])
  (:require-macros
   [devcards.core :refer [defcard]]))

(defcard instructions
  (sab/html
    [:h1 "Instructions"
      [:p "After connecting the proto REPL, we need to execute these commands
           to connect to the figwheel repl."]
      [:pre "(use 'figwheel-sidecar.repl-api)\n(cljs-repl)"]]))

(defcard basic-path
  (sab/html
    [:h1 "Path test"
     [:p "Here is a linear path between points"]
     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/corners
                     [
                       [80 100]
                       [100 80]
                       [120 100]]})]
     [:p "The same thing, but with curves; the circles show the handles"]
     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/corners
                     [
                       [80  100   0   0  80 80]
                       [100  80  80  80 120 80]
                       [120 100 120  80   0  0]]})
       (for [pos [[80 80] [120 80]]]
         (object->svg {::d/object-type :circle
                       ::d/id 1
                       ::d/position pos
                       ::d/radius 2}))]


     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/corners
                     [
                       [10  10   0  0 20 20]
                       [50  10  40 20 50 10]
                       [90  10  90 10]]})
       (for [pos [[20 20] [40 20] [50 10] [90 10]]]
         (object->svg {::d/object-type :circle
                       ::d/id 1
                       ::d/position pos
                       ::d/radius 2}))]
     [:p
      "This "
      [:a {:href "https://codepen.io/anthonydugois/pen/mewdyZ"}
        "codepen"]
      " is very useful for testing SVG paths"]]))

(defcard visualize-anchors
  (sab/html
    [:h1 "Visualize anchors test"
     [:p "Here we are the render function to visualize the first and last anchors"]

     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/display-anchors [0 2]
                     ::d/corners
                     [
                       [10  10   0  0 20 20]
                       [50  10  40 20 50 10]
                       [90  10  90 10]]})]]))

(defcard visualize-handles
  (sab/html
    [:h1 "Visualize handles test"
     [:p "Here we are the render function to visualize the first and last anchors"]

     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/display-handles [1]
                     ::d/corners
                     [
                       [10  10   0  0 20 20]
                       [50  10  40 20 50 10]
                       [90  10  90 10]]})]]))
