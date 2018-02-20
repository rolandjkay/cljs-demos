(ns parabola.objects-cards
  (:require
   [sablono.core :as sab] ; just for example
   [parabola.objects :refer [object->svg]]
   [parabola.domain :as d]
   [clojure.spec.alpha :as s])
  (:require-macros
   [devcards.core :refer [defcard]]))

(defcard instructions
  (sab/html
    [:div
      [:h1 "Instructions"]
      [:p "After connecting the proto REPL, we need to execute these commands
           to connect to the figwheel repl."]
      [:pre "(use 'figwheel-sidecar.repl-api)\n(cljs-repl)"]
      [:p
        "This "
        [:a {:href "https://codepen.io/anthonydugois/pen/mewdyZ"} "codepen"]
        " is very useful for testing SVG paths"]]))

(defcard linear-path
  (sab/html
    [:div
      [:h1 "Path test"]
      [:p "Here is a linear path between points"]
      [:svg {:width "200" :height "200"}
        (object->svg {::d/object-type :path
                      ::d/id 0
                      ::d/vertices
                      [
                        {::d/vertex-type :no-handles ::d/position [80  100]}
                        {::d/vertex-type :no-handles ::d/position [100  80]}
                        {::d/vertex-type :no-handles ::d/position [120 100]}]})]]))

(defcard parabolic-path
  (sab/html
    [:div
     [:p "Two parabolic path segments with circles to show the handles"]
     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/vertices
                     [
                       {::d/vertex-type :handle-after  ::d/position [80 100]  ::d/after-angle -90 ::d/after-length 20}
                       {::d/vertex-type :symmetric     ::d/position [100 80]  ::d/angle  0 ::d/length 20}
                       {::d/vertex-type :handle-before ::d/position [120 100] ::d/before-angle 90 ::d/before-length 20}]})
       (for [pos [[80 80] [120 80]]]
         (object->svg {::d/object-type :circle
                       ::d/id 1
                       ::d/position pos
                       ::d/radius 2}))]]))

(defcard curve-and-line
  (sab/html
    [:div
     [:p "A curve and a line with circles to show handles"]
     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/vertices
                     [
                       {::d/vertex-type :handle-after ::d/position [10 10] ::d/after-angle 45 ::d/after-length 14}
                       {::d/vertex-type :handle-before ::d/position [50 10] ::d/before-angle -45 ::d/before-length 14}
                       {::d/vertex-type :no-handles ::d/position [90 10]}]})
       (for [pos [[20 20] [40 20] [50 10] [90 10]]]
         (object->svg {::d/object-type :circle
                       ::d/id 1
                       ::d/position pos
                       ::d/radius 2}))]]))


(defcard visualize-anchors
  (sab/html
    [:div
      [:h1 "Visualize anchors test"]
      [:p "Here we use the render function to visualize the first and last anchors"]

      [:svg {:width "200" :height "200"}
        (object->svg {::d/object-type :path
                      ::d/id 0
                      ::d/display-anchors [0 2]
                      ::d/vertices
                      [
                        {::d/vertex-type :handle-after ::d/position [10 10] ::d/after-angle 45 ::d/after-length 14}
                        {::d/vertex-type :handle-before ::d/position [50 10] ::d/before-angle -45 ::d/before-length 14}
                        {::d/vertex-type :no-handles ::d/position [90 10]}]})]]))

(defcard visualize-handles
  (sab/html
    [:div
     [:h1 "Visualize handles test"]
     [:p "It looks pretty silly on the first point because we have an unused handle that gets rendered"]
     [:svg {:width "200" :height "200"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/display-handles [0]
                     ::d/vertices
                     [
                       {::d/vertex-type :handle-after ::d/position [60 60] ::d/after-angle -45 ::d/after-length 28}
                       {::d/vertex-type :handle-before ::d/position [100 60] ::d/before-angle 18 ::d/before-length 32}
                       {::d/vertex-type :no-handles ::d/position [140 60]}]})]]))
                       ;[60  60   0  0  80 80]
                       ;[100 60  90 70 100 60]
                       ;[140 60 140 60]]})]]))


(defcard visualize-handles-2
  (sab/html
    [:div
     [:h1 "Visualize handles test"]
     [:p "Here we are the render function to visualize the first and last anchors"]

     [:svg {:width "400" :height "400"}
       (object->svg {::d/object-type :path
                     ::d/id 0
                     ::d/display-handles [0 1 2]
                     ::d/vertices
                     [
                      {::d/vertex-type :handle-after ::d/position [100 300] ::d/after-angle -90 ::d/after-length 50}
                      {::d/vertex-type :asymmetric ::d/position [150 350] ::d/before-angle -45 ::d/before-length 141 ::d/after-angle 117 ::d/after-length 112}
                      {::d/vertex-type :handle-before ::d/position [300 300] ::d/before-angle 90 ::d/before-length 100}]})]]))
                      ; [100 300   0   0 100 250]
                      ; [150 350 250 250 250 400]
                      ; [300 300 300 400]]})]]))
