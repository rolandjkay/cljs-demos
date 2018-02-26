(ns parabola.tools
  (:require [re-frame.core :as re-frame]))


(defprotocol ITool
  "A protocol for tools that re-frame uses to forward interactions"
  (click [this position] "The canvas was clicked"))

;;
;; Create circle tool
;;

(defrecord MakeCircle []
    ITool
    (click [this position]
      (re-frame/dispatch [:objects/create-circle position 20])))

;;
;; A map of all our tools
;;

(def tools-map {:tools/circle-add (MakeCircle.)})

;; The tools translate low-level events, such as mouse down on object, into
;; higher level events, such as delete object or move object.
;;

;(defprotocol ITool
;  "A protocol for tools"
;  (on-mouse-down-on-object [this object-type object-id client-position] "Handle mouse down event on an object")
;  (on-mouse-down-on-background [this client-position] "Handle mouse down event on the background")
;  (on-mouse-up-on-object [this object-type object-id] "Handle mouse up event on an object")
;  (on-mouse-up-on-background [this] "Handle mouse up event on the background")
;  (on-mouse-out-of-object [this object-type object-id] "Handle mouse out event on an object")
;  (on-mouse-out-of-background [this] "Handle mouse out event on the background"))
;
;(defrecord MoveTool []
;    ITool
;    (on-mouse-down-on-object [this object-type object-id client-position]
;      (re-frame.core/dispatch [:select-object object-type object-id client-position])
;
;    (on-mouse-down-on-background [this] (println "Move tool ignoring mouse down on background"))
;    (on-mouse-up-on-object [this object-type object-id client-position] (println "Delete tool ignoring mouse up on object"))
;    (on-mouse-up-on-background [this] (println "Delete tool ignoring mouse up on background"))
;    (on-mouse-out-of-object [this object-type object-id] (println "Delete tool ignoring mouse out of object"))
;    (on-mouse-out-of-background [this] (println "Delete tool ignoring mouse out of background")))


;(defrecord DeleteTool []
;    ITool
;    (on-mouse-down-on-object [this object-type object-id client-position]
;      (re-frame.core/dispatch [:delete object-type object-id])
;
;    (on-mouse-down-on-background [this client-position] (println "Delete tool ignoring mouse down on background"))
;    (on-mouse-up-on-object [this object-type object-id] (println "Delete tool ignoring mouse up on object"))
;    (on-mouse-up-on-background [this] (println "Delete tool ignoring mouse up on background"))
;    (on-mouse-out-of-object [this object-type object-id] (println "Delete tool ignoring mouse out of object"))
;    (on-mouse-out-of-background [this] (println "Delete tool ignoring mouse out of background")))
