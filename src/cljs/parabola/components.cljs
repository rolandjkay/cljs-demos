;;
;; Reagent components
;;
(ns parabola.components
  (:require [parabola.objects :as obj]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add pos-diff split-id valid?]]
            [clojure.spec.alpha :as s]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cljsjs.interact]
            [clojure.string :as str]
            [parabola.log :as log]
            [parabola.subs :as subs]))

(defn- install-handler
  "Installs interactsj handler

  Return Interactable object, or nil of failure
  "
  [this handler]
  (some-> ".drag-me"
    (js/interact {:context (reagent/dom-node this)})
    (.draggable true)
    (.on "tap" (fn [e] (println "<<< tapped >>>")))
    (.on "dragmove"
      (fn [e]
        (let [target-id (-> e .-target .-id)
              move-vec [(.-dx e) (.-dy e)]]
          (if handler
              (handler target-id move-vec)
              (log/warn "No on-dragmove attrib in component")))))))


(defn make-draggable
  "A HOC which endows a simple component with selection and drag of SVG elements"
  [wrapped-component on-dragmove-fn]
  {:pre [(valid? fn? on-dragmove-fn)]}

  (reagent/create-class
    {:component-did-mount #(if (install-handler % on-dragmove-fn) nil (log/warn "Failed to install interactjs handler"))
     :display-name  "draggable-component"  ;; for more helpful warnings & errors
     :reagent-render wrapped-component}))

;; A component which, given one or more objects, allow the user to edit them.
;;
(defn edit-given-component
  "A component which makes the given objects editable"
  [initial-objects]
  {:pre [(s/valid? ::d/objects initial-objects)]}

  (let [objects (reagent/atom initial-objects)]
    (make-draggable
      ;; Renderer
      (fn []
        [:svg {:width "400"
               :height "400"}
          (map obj/object->svg @objects)])

      ;; Drag handler
      (fn [target-id move-vec] {:pre [(s/valid? ::d/dom-id target-id)]}
        (let [[obj-index & rest] (split-id target-id)]
          (swap! objects
            update obj-index ; <- @objects gets passed as first arg; so this runs func below on ith element on objects
            obj/object-with-node-moved rest (partial pos-add move-vec)))))))

;; An SVG-canvas control which dispatches re-frame events.
;;
(defn svg-canvas
  "An SVG-canvas control which dispatches re-frame events and visualizes the
   objects in the application state.
  "
  [{:keys [props] :or {props {}}}]
  (println props (get props :width "800"))
  (make-draggable
    ;; Renderer
    (fn []
      [:svg {:width (get props :width "800"),
             :height (get props :height "800"),
             :on-click
             (fn svg-canvas-on-click-handler [e]
               (let [rect (-> e .-target .getBoundingClientRect)];)
                 (re-frame/dispatch
                   [:canvas/click [(- (.-clientX e) (.-left rect))
                                   (- (.-clientY e) (.-top rect))]])))}
        (map obj/object->svg @(re-frame/subscribe [:subs/objects]))])

    ;; Drag handler
    (fn [target-id move-vec])))
;      {:pre [(valid? ::d/dom-id target-id) (valid? ::d/position move-vec)]}
;      (re-frame/dispatch [:canvas/dragmove (split-id target-id) move-vec]))))

(defn toolbar
  "A toolbar component"
  []
  (let [selected-tool (re-frame/subscribe [:subs/selected-tool]),
        tool-button
        (fn [kw label]
          [:button {:type "button"
                    :class (if (= @selected-tool kw) "active btn-primary" "btn-primary")
                    :on-click #(re-frame.core/dispatch [:cmds/select-tool kw])}
            label])]

    [:div#toolbar.btn-group-vertical.btn-group-lg
      [:div#nodes
       [:p "Nodes"]
       (tool-button :tools/node-move "Move")
       (tool-button :tools/node-delete "Delete")
       (tool-button :tools/node-add "Add")]

      [:div#objects
       [:p "Objects"]
       (tool-button :tools/object-move "Move")
       (tool-button :tools/object-delete "Delete")
       (tool-button :tools/circle-add "Circle")]]))
