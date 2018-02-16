(ns parabola.views
  (:require [re-frame.core :as re-frame]
            [reagent.format :as format]
            [parabola.subs :as subs]))



;; home

(defn controls [start-point]
  [:input {:type "range" :name "range" :min "1" :max "100" :value (:x start-point)
           :on-change #(re-frame.core/dispatch [:set-start-point-x (.-value (.-target %1))])}])

(defn handle
  "Render a handle with the give ID at the given positions

  e.g. (handle {:x 100 :y 100} {:x 100 :y 100} true :third)
  "
  [controlled-point position selected id]
  [:g {:key id}
    ;; Dashed line from path to handle
    [:path {:fill "none"
            :stroke "black"
            :stroke-dasharray "5,5"
            :d (format/format "M %d,%d L %d,%d"
                 (:x controlled-point) (:y controlled-point)
                 (:x position) (:y position))}]

    ;; The handle
    [:circle {:cx (:x position) :cy (:y position) :r "5"
              :fill (if selected "red" "black")
              :on-mouse-down #(re-frame.core/dispatch
                                   [ :select-object
                                     :handle id
                                     {:x (.-clientX %1) :y (.-clientY %1)}])}]])

(defn anchor
  "Render an anchor point"
  [anchor-point selected id]
  (let [{:keys [x y]} anchor-point]
    [:g {:key (str "a-" id)}
      [:polygon {:fill (if selected "cyan" "black")
                 :points (str (- x 3) "," y " "
                              x "," (+ y 3) " "
                              (+ x 3) "," y " "
                              x "," (- y 3) " "
                              (- x 3) "," y)
                 :on-mouse-down #(re-frame.core/dispatch
                                     [ :select-object
                                       :anchor id
                                       {:x (.-clientX %1) :y (.-clientY %1)}])}]]))


(defn graph [handle-point-pairs
             selected-object]
  ; We need to transform the sequenece of handle/point pairs into an SVG
  ; path. Most pairs just map to an SVG "S" command. However, the first two
  ; pairs are a special case, as we need an M and a C at the start to get the
  ; path going.
  ;
  ; p1 h1   -> M p1 C h1
  ; p2 h2   -> h2 p2       <- Second half of 'C' command
  ; p3 h3   -> S h3 p3
  ; p4 h4   -> S h4 p4
  ;
  (defn map-first-pair [[handle point]]
    (str "M " (:x point) "," (:y point) " C " (:x handle) "," (:y handle) " "))

  (defn map-second-pair [[handle point]]
    (str (:x handle) "," (:y handle) " " (:x point) "," (:y point) " "))

  (defn map-pair [pair]
    (str "S" (map-second-pair pair)))

  ;; Use a Reduce to map map-first-pair, map-second-pair and map-pair over
  ;; the list of handle/point pairs and concatonate into a string
  (let [path
          (reduce #(str %1 (map-pair %2))
            ; Initial values (special case for first two pairs)
            (str (map-first-pair (first handle-point-pairs)) (map-second-pair (second handle-point-pairs)))
            ; Apply generic mapping to the rest of the pairs
            (rest (rest handle-point-pairs)))]

    [:svg {:width "200" :height "200"
           :on-mouse-move #(re-frame.core/dispatch [:mouse-move {:x (.-clientX %1) :y (.-clientY %1)}])
           :on-mouse-up #(re-frame.core/dispatch [:select-object :none])
           ;; This is a rubbish way to filter the mouse-out events to just the parent SVG element.
           :on-mouse-out #(let [rt (.. %1 -relatedTarget -nodeName)]
                            (if (= "DIV" rt)
                                (re-frame.core/dispatch [:select-object :none])))}

      [:path {:fill "none" :stroke "black" :d path}]

      ; Enumerate over sequenece of handle/point pairs; we using the index
      ; as the ID of the handle.
      (map
        (fn [index [handle* point]]
            (handle point handle*
              (and (= (:object-type selected-object) :handle)
                   (= (:object-index selected-object) index))
              index))
        (range)
        handle-point-pairs)

      ; anchors
      (map
        (fn [index [handle* point]]
            (anchor point
              (and (= (:object-type selected-object) :anchor)
                   (= (:object-index selected-object) index))
              index))
        (range)
        handle-point-pairs)]))


(defn home-panel []
  (let [name (re-frame/subscribe [::subs/name])
        anchor-points (re-frame/subscribe [:anchor-points])
        selected-object (re-frame/subscribe [:selected-object])
        handle-positions (re-frame/subscribe [:handle-positions])]

    [:div (str "Hello from " (get-in @handle-positions [2 :x]) ". This is the Home Page.")
     [:div [:a {:href "#/about"} "go to About Page"]
       [:div#canvas
         (graph
           ; Zip handles and anchors together
           (map #(vector %1 %2) @handle-positions @anchor-points)
           @selected-object)]
       [:div#add-button
         [:button {:on-click #(re-frame/dispatch [:add-anchor])}
           "Add anchor"]]
       [:div#add-button
         [:button {:on-click #(re-frame/dispatch [:select-delete-anchor-tool])}
           "Delete anchor"]]]]))

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
