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



;; A component which, given one or more objects, allow the user to edit them.
;;
;;

(defn- make-edit-given-component-dragmove-handler
  [objects]
  (fn [target-id move-vec] {:pre [(s/valid? ::d/dom-id target-id)]}
    (let [[obj-index & rest] (split-id target-id)]
      (swap! objects
        update obj-index ; <- @objects gets passed as first arg; so this runs func below on ith element on objects
        obj/object-with-node-moved rest (partial pos-add move-vec)))))


(defn edit-given-component
  "A component which makes the given objects editable"
  [initial-objects]
  {:pre [(s/valid? ::d/objects initial-objects)]}

  (let [objects (reagent/atom initial-objects)]
    (print ":::" @objects)
    (fn []
      [:svg {:width "400"
             :height "400"
             :on-dragmove (make-edit-given-component-dragmove-handler objects)}
        (map obj/object->svg @objects)])))
