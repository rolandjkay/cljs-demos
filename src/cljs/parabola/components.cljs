;;
;; Reagent components
;;
(ns parabola.components
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [parabola.objects :as obj]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add pos-diff str->id valid?]]
            [clojure.spec.alpha :as s]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cljsjs.interact]
            [clojure.string :as str]
            [parabola.log :as log]
            [parabola.subs :as subs]
            [parabola.ml :as ml]
            [parabola.highlight :as highlight]
            [parabola.assets :as assets]))

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
         (let [[obj-index & rest] (str->id target-id)]
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
          (let [id (.-id e)]
            (cond
              (= id "canvas")            nil ,   ; <- give up when we get to the canvas.
              (s/valid? ::d/dom-id id)   (str->id id),
              (nil? (.-parentElement e)) nil ,   ; <- or if we get to the DOM root!
              :else                      (recur (.-parentElement e))))))

      ;; Event handlers
      {
       ;; Drag handler
       :dragmove
       (cljs.core/fn [target-id move-vec]
         {:pre [(valid? ::d/dom-id target-id) (valid? ::d/position move-vec)]}
         (re-frame/dispatch [:canvas/drag move-vec (str->id target-id)]))

       ;; move handler
       :canvas-move
       (cljs.core/fn [position] {:pre [(valid? ::d/position position)]}
         (re-frame/dispatch [:canvas/move position]))

       ;; tap handler
       :canvas-click
       (cljs.core/fn [position id-path]
         {:pre [(valid? ::d/position position)
                (valid? (s/nilable ::d/id-path) id-path)]}
         (re-frame/dispatch [:canvas/click position id-path]))

       ;; doubletap handler
       :canvas-double-click
       (cljs.core/fn [position obj-id]
         {:pre [(valid? ::d/position position)
                (valid? (s/nilable ::d/id-path) obj-id)]}
         (re-frame/dispatch [:canvas/double-click position obj-id]))})))


(defn toolbar
  "A toolbar component"
  []
  (let [selected-tool (re-frame/subscribe [:subs/selected-tool]),
        tool-button
        ;(fn [kw label]
        ;  [:button {:type "button"
        ;            :class (if (= @selected-tool kw) "active btn-primary" "btn-primary")
        ;            :on-click #(re-frame.core/dispatch [:cmds/select-tool kw])
        ;    label]
        (fn [kw label icon]
          [:button.btn.btn-secondary
            {:type "button"
             :class (if (= @selected-tool kw) "active")
             :on-click #(re-frame.core/dispatch [:cmds/select-tool kw])}
            icon
            [:br]
            label])]

;;     <div class="btn-toolbar mb-3" role="toolbar" aria-label="Toolbar with button groups">
;;      <div class="btn-group-vertical mr-2" role="group" aria-label="First group">
;;        <button type="button" class="btn btn-secondary"><i class="fas fa-eraser fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="fas fa-arrows-alt fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="fas fa-mouse-pointer fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="far fa-circle fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="fas fa-pencil-alt fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="far fa-square fa-3x"></i></button>
;;        <button type="button" class="btn btn-secondary"><i class="fas fa-circle fa-sm"></i></button>]
;;      </div>
;;    </div>
    [:div.btn-toolar.mb-3 {:role "toolbar" :aria-label "Tool box"}

      [:div.btn-group-vertical.mr-2 {:role "group" :aria-label "Node tools"}
        [:a {:href "#" :role "button" :class "btn btn-success btn-xs"
             :id "label-btn" :aria-disabled "true"
             :style {:pointer-events "none"}}
          "Nodes"]

        (tool-button :tools/node-move "Move" [:i.fas.fa-arrows-alt.fa-1x])
        (tool-button :tools/node-delete "Delete" [:i.fas.fa-eraser.fa-1x])
        (tool-button :tools/node-add "Add" [:i.fas.fa-mouse-pointer.fa-1x])
        (tool-button :tools/node-select "Select" [:i.fas.fa-mouse-pointer.fa-1x])]

      [:div.btn-group-vertical.mr-2 {:role "group" :aria-label "Object tools"}
        [:a {:href "#" :role "button" :class "btn btn-success btn-xs"
             :id "label-btn" :aria-disabled "true"
             :style {:pointer-events "none"}}
          "Objects"]

        (tool-button :tools/object-move "Move" [:i.fas.fa-arrows-alt.fa-1x])
        (tool-button :tools/object-delete "Delete" [:i.fas.fa-eraser.fa-1x])
        (tool-button :tools/make-circle "Circle" [:i.fas.fa-circle.fa-1x])
        (tool-button :tools/make-path "Path" [:i.fas.fa-pencil-alt.fa-1x])]]))

;    [:div#toolbar.btn-group-vertical.btn-group-lg
;      [:div#nodes
;       [:p "Nodes"]
;       (tool-button :tools/node-move "Move")
;       (tool-button :tools/node-delete "Delete")
;       (tool-button :tools/node-add "Add")
;       (tool-button :tools/node-select "Select")
;
;      [:div#objects
;       [:p "Objects"]
;       (tool-button :tools/object-move "Move")
;       (tool-button :tools/object-delete "Delete")
;       (tool-button :tools/make-circle "Circle")
;       (tool-button :tools/make-path "Path")]))


(defn properties-bar
  "A properties toolbar for changing anchor types"
  []
  (let [vertex-selected? (re-frame/subscribe [:subs/node-selected?]),
        tool-button
        (fn [kw label icon]
          [:button.btn.btn-secondary
              {:type "button"
               :class (if @vertex-selected? "" "disabled")
               :on-click #(re-frame.core/dispatch [:cmds/set-vertex-type kw])}
            icon
            [:br]
            label])]

    [:div.btn-toolar.mb-3 {:role "toolbar" :aria-label "Node properties"}
      [:div.btn-group-vertical.mr-2 {:role "group" :aria-label "Node types"}
        [:a {:href "#" :role "button" :class "btn btn-success btn-xs"
             :id "label-btn" :aria-disabled "true"
             :style {:pointer-events "none"}}
          "Node Types"]

        (tool-button :no-handles "Corner"  (assets/node-no-handles))
        (tool-button :handle-before "Before" (assets/node-handle-before))
        (tool-button :handle-after "After" (assets/node-handle-after))
        (tool-button :semi-symmetric "Smooth" (assets/node-semi-symmetric))
        (tool-button :symmetric "Symmetric" (assets/node-symmetric))
        (tool-button :asymmetric "Asymmetric" (assets/node-asymmetric))]]))

;;
;; Display objects as XML
;;

;(defn- markup-display-render []
;  (let [objects (re-frame/subscribe [:subs/objects])]
;    [:pre
;      [:code
;        (ml/objects->markup @objects))]))
;
;(defn- highlight-code [html-node]
;  ;(let [nodes (.querySelectorAll html-node "pre code")]
;  ;  (loop [i (.-length nodes)]
;  ;    (when-not (neg? i)
;  ;      (when-let [item (.item nodes i)]
;          (.highlightBlock js/hljs html-node))]))
;  ;      (recur (dec i)))
;
;(defn- markup-display-did-mount [this]
;  (let [node (reagent/dom-node this)]
;    (highlight-code node)))

;(defn markup-display []
;  (reagent/create-class
;   {:reagent-render      markup-display-render
;    :component-did-mount markup-display-did-mount))

(defn markup-display
  "Display the objects as a simplified markup"
  []
  (let [objects (re-frame/subscribe [:subs/objects])]
    [:div
      (highlight/highlight
        (ml/objects->markup @objects))]))
