;;
;; Reagent components
;;
(ns parabola.components
  (:require [parabola.objects :as obj]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add pos-diff split-id valid?]]
            [clojure.spec.alpha :as s]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [parabola.log :as log]))

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
    (make-draggable
      ;; Renderer
      (fn []
        [:svg {:width "400"
               :height "400"}
          (map obj/object->svg @objects)])
      ;; Drag handler
      (make-edit-given-component-dragmove-handler objects))))
