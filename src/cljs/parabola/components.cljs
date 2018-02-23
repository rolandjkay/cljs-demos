;;
;; Reagent components
;;
(ns parabola.components
  (:require [parabola.objects :as obj]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add pos-diff split-id]]
            [clojure.spec.alpha :as s]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [adzerk.cljs-console :as log :include-macros true]))

(defn- install-handler
  "Installs interactsj handler

  Return Interactable object, or nil of failure
  "
  [this handler]
  (some-> ".drag-me"
    (js/interact {:context (reagent/dom-node this)})
    (.draggable true)
    (.on "dragmove"
      (fn [e]
        (let [target-id (-> e .-target .-id)
              move-vec [(.-dx e) (.-dy e)]]
          (if handler
              (handler target-id move-vec)
              (log/warn "No on-dragmove attrib in component")))))))

; XXX I don't like that we have to care what type of component it is.
; Although, I don't see a generic way to get the handlers out of it.

;
; [:div]   ->   hiccup = component, renderer = (fn [] hiccup)
; [comp] (defn comp [] [:div])  -> hiccup = (comp), renderer = comp
; [comp] (defn comp [] (fn))    -> renderer = (comp), hiccup = (renderer)

;; This only works for a form-2 component; i.e. one which consists of
;; a ctor function that returns a render function.
;;
(defn make-draggable
  "A HOC which endows a simple component with selection and drag of SVG elements"
  [component]
;  (let [tag            (first component)
;        ; XXX This is ridiculous because we
;        [instance hiccup]
;        (cond
;          (keyword? tag) [(fn [] component) component]
;;          (and (fn? tag)
; ;              (keyword? (first (tag)))) [component (tag)]
;;        :else                          (let [x (tag)]
;                                           x (x)
;        [tag props]    hiccup
;        on-dragmove-fn (:on-dragmove props)

  (let [[ctor & args] component
        instance (apply ctor args)
        hiccup  (instance)
        [tag props]    hiccup
        on-dragmove-fn (:on-dragmove props)]

     (reagent/create-class
       {:component-did-mount #(if (install-handler % on-dragmove-fn) nil (log/warn "Failed to install interactjs handler"))
        :display-name  "draggable-component"  ;; for more helpful warnings & errors
        :reagent-render instance})))

(defn make-draggable-form-1
  "A HOC which endows a simple component with selection and drag of SVG elements"
  [hiccup]

  (let [[tag props]    hiccup
        on-dragmove-fn (:on-dragmove props)]

     (reagent/create-class
       {:component-did-mount #(if (install-handler % on-dragmove-fn) nil (log/warn "Failed to install interactjs handler"))
        :display-name  "draggable-component"  ;; for more helpful warnings & errors
        :reagent-render (fn [] hiccup)})))

;
;{::d/object-type :path
;              ::d/id 0
;              ::d/display-anchors [0 1 2]
;              ::d/selected-anchors [0 2]
;              ::d/vertices
;              [
;               {::d/vertex-type :handle-after ::d/position [100 300] ::d/after-angle 180 ::d/after-length 50}
;               {::d/vertex-type :asymmetric ::d/position [150 350] ::d/before-angle -15 ::d/before-length 141 ::d/after-angle 45 ::d/after-length 62}
;               {::d/vertex-type :handle-before ::d/position [300 300] ::d/before-angle 90 ::d/before-length 100}})))
;

(defn- make-path-component-dragmove-handler
  [state]
  (fn [target-id move-vec] {:pre [(s/valid? ::d/dom-id target-id)]}
    (let [[obj-index _ :as ids] (split-id target-id)]
      (if (= obj-index 0)
        (swap! state
          #(obj/object-with-node-moved % (rest ids) (partial pos-add move-vec)))

        ; default
        (println "Ignoring dragmove on unsupported ID " target-id)))))


(defn path-component
  "A component which makes the given path editable"
  [init-path]
  (let [path (reagent/atom init-path)]
    (fn []
      [:svg {:width "400"
             :height "400"
             :on-dragmove (make-path-component-dragmove-handler path)}
        (obj/object->svg @path)])))
