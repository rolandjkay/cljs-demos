(ns parabola.tools
  (:require [re-frame.core :as re-frame]
            [parabola.log :as log]
            [parabola.domain :as d]
            [parabola.objects :as objects]
            [parabola.utils :refer [pos-add valid? map-function-on-map-vals]]
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]))


;;
;; Functions used by lots of tools
;;

(defn- show-all-anchors [db]
  (update db ::d/objects map-function-on-map-vals #(assoc % ::d/display-anchors :all)))

(defn- hide-all-anchors [db]
  (update db ::d/objects map-function-on-map-vals #(dissoc % ::d/display-anchors)))

(defn- show-all-handles [db]
  (println "HELLO")
  (update db ::d/objects map-function-on-map-vals #(assoc % ::d/display-handles :all)))

(defn- hide-all-handles [db]
  (update db ::d/objects map-function-on-map-vals #(dissoc % ::d/display-handles)))


;;
;; ITool protocol
;;

(defprotocol ITool
  "A protocol for tools that re-frame uses to forward interactions"
  (on-selected [this db] "The tool was selected")
  (on-unselected [this db] "The tool was unselected")
  (on-click [this db position id-path] "The canvas was clicked")
  (on-double-click [this db position obj-id] "The canvas was double clicked")
  (on-move [this db position] "The mouse moved over the canvas")
  (on-drag [this db dpos id-path] "An object was dragged"))


;;
;; Create circle tool
;;

(defrecord MakeCircle []
    ITool
    (on-selected [this db] (log/info "Make circle tool selected") db)
    (on-unselected [this db] (log/info "Make circle tool unselected") db)
    (on-click [this db position id-path]
      (let [next-object-id  (::d/next-object-id db)]
        (-> db
          (assoc-in [::d/objects next-object-id]
            {::d/object-type :circle,
             ::d/id next-object-id,
             ::d/position position,
             ::d/radial [20 0]})
          (update ::d/next-object-id inc))))

    (on-double-click [this db position id-path] db)
    (on-move [this db position] db)
    (on-drag [this db dpos obj-id] db))


;;
;; Create path tool
;;

(defn- new-path
  "Create a new stub path for the make path tool"
  [id position]
  {::d/object-type :path,
   ::d/id id,
   ::d/display-anchors :all,
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
      (assoc db ::d/tool-state nil))

    (on-unselected [this db] (log/info "Make path tool unselected")
      ;; Destroy our state
      (assoc db ::d/tool-state nil))

    (on-click [this db position _]
      (let [id-path (get-in db [:db/tool-state ::d/id]),
            next-object-id  (::d/next-object-id db)]
        (if (nil? id-path)
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
            (update-in db [::d/objects id-path ::d/vertices] conj {::d/vertex-type :no-handles ::d/position position}))))

    (on-double-click [this db position _]
      ;; If we were creating a path; stop.
      (let [id-path (get-in db [:db/tool-state ::d/id]),]
        (if (nil? id-path)
            db ; Do nothing
            (assoc-in db [:db/tool-state ::d/id] nil))))

    ;; If we have a stub-path in progress then moving the mouse should move the
    ;; last point.
    (on-move [this db position]
      (let [id-path (get-in db [:db/tool-state ::d/id])]
        (if (nil? id-path) db  ; Do nothing))))
            ;; Move the last point
            (update-in db [::d/objects id-path ::d/vertices] with-last-point-moved position))))

    (on-drag [this db dpos obj-id] db))

;;
;; Delete object tool
;;

(defrecord DeleteObject []
    ITool
    (on-selected [this db]
      (show-all-anchors db))

    (on-unselected [this db]
      (hide-all-anchors db))

    (on-click [this db position [obj-id & sub-id-path]]
      (if-not obj-id db (update db ::d/objects dissoc obj-id)))

    (on-double-click [this db position id-path] db)
    (on-move [this db position] db)
    (on-drag [this db dpos obj-id] db))



;;
;; Move object tool
;;


(defrecord MoveObject []
    ITool
    (on-selected [this db]
      (show-all-anchors db))

    (on-unselected [this db]
      (hide-all-anchors db))

    (on-click [this db position id-path] db)

    (on-double-click [this db position id-path] db)
    (on-move [this db position] db)

    (on-drag [this db dpos id-path]
      ;; XXX We are inconsistent; on-click takes a simple object ID (2) whereas
      ;;     on-drag takes a complete path [1 2 3]
      ;; XXX We should always use the path.
      (update-in
        db
        [::d/objects (first id-path)]
        objects/moved-object (partial pos-add dpos))))


;;
;; Move node (anchor or handle) tool
;;

(defrecord MoveNode []
    ITool
    (on-selected [this db]
      (-> db show-all-anchors show-all-handles))

    (on-unselected [this db]
      (-> db hide-all-anchors hide-all-handles))

    (on-click [this db position obj-id] db)

    (on-double-click [this db position id-path] db)
    (on-move [this db position] db)

    (on-drag [this db dpos id-path]
      ;; XXX We are inconsistent; on-click takes a simple object ID (2) whereas
      ;;     on-drag takes a complete path [1 2 3]
      ;; XXX We should always use the path.
      (let [[obj-index & rest] id-path]
        (cond
          (not (int? obj-index)) (do (log/warn "Invalid object index") db)
          (empty? rest)          db
          :else
          (update-in
            db [::d/objects obj-index]
            objects/object-with-node-moved rest (partial pos-add dpos))))))

;;
;; Delete node (anchor or handle) tool
;;

(defrecord DeleteNode []
    ITool
    (on-selected [this db]
      (-> db show-all-anchors show-all-handles))

    (on-unselected [this db]
      (-> db hide-all-anchors hide-all-handles))

    (on-click [this db position id-path] db
      ;; Did they click on a node?
      (println id-path)
      db)

    (on-double-click [this db position id-path] db)
    (on-move [this db position] db)

    (on-drag [this db dpos obj-id] db))


;;
;; A map of all our tools
;;

(def tools-map {:tools/make-circle (MakeCircle.)
                :tools/make-path (MakePath.)
                :tools/object-delete (DeleteObject.)
                :tools/object-move (MoveObject.)
                :tools/node-move (MoveNode.)
                :tools/node-delete (DeleteNode.)})
