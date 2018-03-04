(ns parabola.tools
  (:require [re-frame.core :as re-frame]
            [parabola.log :as log]
            [parabola.domain :as d]
            [parabola.utils :refer [pos-add valid?]]
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]))


(defprotocol ITool
  "A protocol for tools that re-frame uses to forward interactions"
  (on-selected [this db] "The tool was selected")
  (on-unselected [this db] "The tool was unselected")
  (on-click [this db position] "The canvas was clicked")
  (on-double-click [this db position] "The canvas was double clicked")
  (on-move [this db dpos]))


;;
;; Create circle tool
;;

(defrecord MakeCircle []
    ITool
    (on-selected [this db] (log/info "Make circle tool selected"))
    (on-unselected [this db] (log/info "Make circle tool unselected"))
    (on-click [this db position]
      (re-frame/dispatch [:objects/create-circle position 20]))
    (on-double-click [this db position])
    (on-move [this db dpos]))

;;
;; Create path tool
;;

(defn- new-path
  "Create a new stub path for the make path tool"
  [id position]
  {::d/object-type :path,
   ::d/id id,
   ::d/display-anchors (into (vector) (range 100)),
   ::d/vertices
   [
     {::d/vertex-type :no-handles ::d/position position}
     {::d/vertex-type :no-handles ::d/position (pos-add position [10 10])}]})

(defn- with-last-point-moved
  "Return a copy of the path vertices with the last point moved by dpos"
  [vertices new-position]
  {:pre [(valid? ::d/vertices vertices) (valid? ::d/position new-position)]
   :post [(valid? ::d/vertices %)]}

  (conj
    (pop vertices)  ;<- pop doesn't transform to seq, like butlast does
    (assoc (last vertices) ::d/position new-position)))


(defrecord MakePath []
    ITool
    (on-selected [this db]
      ;; Create our state
      (re-frame/dispatch [:db/create-make-path-tool-state]))

    (on-unselected [this db] (log/info "Make circle tool unselected")
      ;; Destroy our state
      (re-frame/dispatch [:db/remove-tool-state]))

    (on-click [this db position]
      (let [path-id (get-in db [:db/tool-state ::d/id]),
            next-object-id  (::d/next-object-id db)]
        (if (nil? path-id)
            ; We are not already creating a path. So, create a new one.
            (-> db
             ; Create path with next-object-id
             (assoc-in [::d/objects next-object-id] (new-path next-object-id position))
             ; store next-object-id in tool state
             (assoc :db/tool-state {::d/tool-state-type :tools/make-path ::d/id next-object-id})
             ; inc next-object-id
             (update ::d/next-object-id inc))

            ; We are in the middle of creating a path.
            ; Add a new vertex
            (update-in db [::d/objects path-id ::d/vertices] conj {::d/vertex-type :no-handles ::d/position position}))))

    (on-double-click [this db position]
      ;; If we were creating a path; stop.
      (let [path-id (get-in db [:db/tool-state ::d/id]),]
        (if (nil? path-id)
            db ; Do nothing
            (assoc-in db [:db/tool-state ::d/id] nil))))

    ;; If we have a stub-path in progress then moving the mouse should move the
    ;; last point.
    (on-move [this db dpos]
      (let [path-id (get-in db [:db/tool-state ::d/id])]
        (if (nil? path-id) db  ; Do nothing))))
            ;; Move the last point
            (update-in db [::d/objects path-id ::d/vertices] with-last-point-moved dpos)))))


;;
;; A map of all our tools
;;

(def tools-map {:tools/make-circle (MakeCircle.)
                :tools/make-path (MakePath.)})

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
