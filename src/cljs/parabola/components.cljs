;;
;; Reagent components
;;
(ns parabola.components
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [parabola.objects :as obj]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add pos-diff split-id valid?]]
            [clojure.spec.alpha :as s]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cljsjs.interact]
            [clojure.string :as str]
            [parabola.log :as log]
            [parabola.subs :as subs]))

(defn- install-handlers
  "Installs interactsj handler

  Return Interactable object, or nil of failure
  "
  [this handlers]
  (some-> ".drag-me"
    (js/interact {:context (reagent/dom-node this)})
    (.draggable true)
    ;(.on "tap" (fn [e] (println "<<< tapped >>>")))
    (.on "dragmove"
      (fn [e]
        (let [target-id (-> e .-target .-id),
              move-vec  [(.-dx e) (.-dy e)],
              handler   (:dragmove handlers)]
          (if handler
              (handler target-id move-vec)
              (log/info "Component ignoring 'dragmove' event")))))))


(defn make-draggable
  "A HOC which endows a simple component with selection and drag of SVG elements"
  [wrapped-component event->obj-id event-handlers]
  {:pre [(valid? (s/map-of keyword? fn?) event-handlers)]}

  (let [top-left (atom [0 0]),
        ; Substitute "do-nothing" handler if missing.
        {:keys [canvas-click canvas-double-click canvas-move]
         :or {canvas-click (fn [position] (log/info "Missing canvas-click handler")),
              canvas-double-click (fn [position] (log/info "Missing canvas-double-click handler")),
              canvas-move (fn [position] (log/info "Missing canvas-move handler"))}}
        event-handlers,
        event->position (fn [e] (let [x (pos-diff [(.-clientX e) (.-clientY e)] @top-left)] x))]

    (reagent/create-class
      {:component-did-mount
        (fn [this]
          (if (install-handlers this event-handlers)
              nil
              (log/warn "Failed to install interactjs handlers"))

          (let [rect (. (reagent/dom-node this) getBoundingClientRect)]
            (reset! top-left [(.-left rect) (.-top rect)])))

       :display-name  "draggable-component"  ;; for more helpful warnings & errors
       :reagent-render
        (fn make-draggable-render []
          (let [click-count (atom 0)]
            [:div {:on-click (fn [e]
                               (swap! click-count inc)
                               (let [position (event->position e)
                                     target-id (event->obj-id e)]
                                 (go
                                   (<! (timeout 300))
                                   (case @click-count
                                     0 nil
                                     1 (canvas-click position target-id)
                                     (canvas-double-click position target-id))
                                   (reset! click-count 0))))
                   :on-mouse-move #(-> % event->position canvas-move)}
              [wrapped-component]]))})))

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

      ;; Event -> object ID
      (fn [e] nil)

       ;; Event handlers
      {
       :dragmove
       (fn [target-id move-vec] {:pre [(s/valid? ::d/dom-id target-id)]}
         (let [[obj-index & rest] (split-id target-id)]
           (swap! objects
             update obj-index ; <- @objects gets passed as first arg; so this runs func below on ith element on objects
             obj/object-with-node-moved rest (partial pos-add move-vec))))})))



;; An SVG-canvas control which dispatches re-frame events.
;;
(defn svg-canvas
  "An SVG-canvas control which dispatches re-frame events and visualizes the
   objects in the application state.
  "
  [{:keys [props] :or {props {}}}]
  (let [foo (atom 0)]
    (make-draggable
      ;; Renderer
      (fn []
        [:svg#canvas
              {:width (get props :width "800"),
               :height (get props :height "800"),}
          (map obj/object->svg @(re-frame/subscribe [:subs/objects]))])

      ;; event to object-id function
      ;; - This depends on our object model; so, it's not generic and can't
      ;;   be in make-draggable
      (fn canvas-event->obj-id [event]
        (loop [e (.-target event)]
          (let [id (.-id e)
                id-int (js/parseInt id)]
            (cond
              (= id "canvas")            nil ,   ; <- give up when we get to the canvas.
              (not (js/isNaN id-int))    id-int, ; parseInt parses "4/1/2" to 4
              (nil? (.-parentElement e)) nil ,   ; <- or if we get to the DOM root!
              :else                      (recur (.-parentElement e))))))

      ;; Event handlers
      {
       ;; Drag handler
       :dragmove
       (fn [target-id move-vec])
   ;      {:pre [(valid? ::d/dom-id target-id) (valid? ::d/position move-vec)]}
   ;      (re-frame/dispatch [:canvas/dragmove (split-id target-id) move-vec]))))

       ;; move handler
       :canvas-move
       (cljs.core/fn [position] {:pre [(valid? ::d/position position)]}
         (re-frame/dispatch [:canvas/move position]))

       ;; tap handler
       :canvas-click
       (cljs.core/fn [position obj-id]
         {:pre [(valid? ::d/position position)
                (valid? (s/nilable int?) obj-id)]}
         (re-frame/dispatch [:canvas/click position obj-id]))

       ;; doubletap handler
       :canvas-double-click
       (cljs.core/fn [position obj-id]
         {:pre [(valid? ::d/position position)
                (valid? (s/nilable int?) obj-id)]}
         (re-frame/dispatch [:canvas/double-click position obj-id]))})))


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
       (tool-button :tools/make-circle "Circle")
       (tool-button :tools/make-path "Path")]]))
