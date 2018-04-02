;;
;; SVG renderable objects
;;
(ns parabola.objects
  (:require [parabola.domain :as d]
            [parabola.utils :refer [value-in-collection? pairs pos-add pos-diff
                                    str->id id->str valid? vector-length
                                    map-function-on-map-vals polar->cartesian
                                    cartesian->polar]
                            :as utils]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [parabola.log :as log]
            [parabola.bezier :as bezier]))


(defn- position->svg
  "Render a position to an SVG string; [1 2]-> \"1,2\"

  Optionally takes an SVG command prefix; e.g.

  (position->svg [1 2] \"M\")
  ;; \"M1,2\"

  "
  ([position] (position->svg position ""))
  ([position cmd]
   {:pre [(s/valid? ::d/position position)
          (string? cmd)]}
  ; (let [x10 (partial * 10)
  ;       x10% (partial * 0.1)
  ;       round (fn [p] (-> p x10 js/Math.round x10%))
   (str cmd (.toFixed (position 0) 1)
        "," (.toFixed (position 1) 1))))

;; This relies on the fact that the first two numbers in the first corner
;; is the anchor position, regardless of the corner type.
(defn- path-first-anchor
  "Map a path to its first point; useful for the initial move command"
  [path]
  {:pre [(s/valid? ::d/path path)]
   :post [(s/valid? ::d/position %)]}
  (::d/position (first (::d/vertices path))))

(log/log-assert (= (path-first-anchor {::d/object-type :path
                                       ::d/id          0
                                       ::d/vertices
                                       [{::d/vertex-type :no-handles ::d/position [100 101]}
                                        {::d/vertex-type :no-handles ::d/position [303 404]}]})
                   [100 101])
                "path-first-anchor failed to fetch first anchor position")

;;; object->svg multi-method ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti
  object->svg
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;;; object->svg [PATH] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- vertex-pair->svg
  "Render a pair of corners to an SVG path line segment"
  [[start end]]
  {:pre [(s/valid? ::d/vertex start)
         (s/valid? ::d/vertex end)]}
  (let [start-position (::d/position start)
        end-position (::d/position end)
        start-type  (::d/vertex-type start)
        end-type  (::d/vertex-type end)
        ; Default the handles to be at the anchor positions if missing.
        ; Try for length/angle first, in case the node is symmetric.
        start-before-length (::d/length start (::d/before-length start 0))
        start-before-angle (::d/angle start (::d/before-angle start 0))
        start-after-length (::d/length start (::d/after-length start 0))
        start-after-angle (::d/angle start (::d/after-angle start 0))
        end-before-length (::d/length end (::d/before-length end 0))
        end-before-angle (::d/angle end (::d/before-angle end 0))
        end-after-length (::d/length end (::d/before-length end 0))
        end-after-angle (::d/angle end (::d/before-angle end 0))]

    ; Special case if there is no handle at the beginning or end of the segment
    ; then draw a line. The generic segment (with C) would work as well in This
    ; case, but the L segment somewhat simplifies the SVG.
    (if (and (= 0 start-after-length)
             (= 0 start-after-angle)
             (= 0 end-before-length)
             (= 0 end-before-angle))
        (position->svg end-position "L")
        (str/join " " [(position->svg (pos-add start-position (polar->cartesian [start-after-length start-after-angle]))
                                      "C")
                       (position->svg (pos-add end-position (polar->cartesian [(- end-before-length) end-before-angle])))
                       (position->svg end-position)]))))

(defn- vertex-anchor->svg
  "Render an anchor"
  [vertex id-path selected large?]
  {:pre [(valid? ::d/vertex vertex)
         (valid? ::d/id-path id-path)
         (valid? boolean? selected)]}

  (let [[x y] (::d/position vertex)
        radius (if large? 6 3)]
    [:g.drag-me {:key (id->str id-path) :id (id->str id-path)}
      [:polygon {:fill  (if selected "#4c7cff" "black")
                 :points (str (- x radius) "," y " "
                              x "," (+ y radius) " "
                              (+ x radius) "," y " "
                              x "," (- y radius) " "
                              (- x radius) "," y)}]]))

(defn- handle->svg
  "Render a single handle"
  [id-path selected anchor-position relative-handle-position]
  {:pre [(s/valid? ::d/position anchor-position)
         (s/valid? ::d/position relative-handle-position)]}
  (let [handle-position (pos-add anchor-position relative-handle-position)
        ; Compose the IDs of the anchor and handle. We attach the anchor
        ; ID to the dashed lines; otherwise, it's hard to select the anchor
        anchor-id (id->str (butlast id-path))
        handle-id (id->str id-path)]

    [:g.drag-me {:key (str handle-id) :id (str handle-id)}
      ;; Dashed line from path to handle
      [:path {:id anchor-id
              :fill "none"
              :stroke "black"
              :stroke-dasharray "5,5"
              :d (str/join " "
                   [(position->svg anchor-position "M")
                    (position->svg handle-position "L")])}]

      ;; The handle
      [:circle {:id handle-id
                :cx (handle-position 0) :cy (handle-position 1) :r "5"
                :fill (if selected "#4c7cff" "black")}]]))


(defn- vertex-handles->svg
  "Render the handles of a vertex"
  {:pre [(s/valid? ::d/vertex vertex)
         (s/valid? (s/coll-f #{:before :after} :kind set?) selection)]}
  [vertex path-id selection]
  (let [position (::d/position vertex)
        type  (::d/vertex-type vertex)]
    (case type
      :no-handles nil
      :handle-before
        (handle->svg (conj path-id 0)
                     (:before selection)
                     position
                     (polar->cartesian [(- (::d/before-length vertex)) (::d/before-angle vertex)]))

      :handle-after
        (handle->svg (conj path-id 1)
                     (:after selection)
                     position
                     (polar->cartesian [(::d/after-length vertex) (::d/after-angle vertex)]))

      :symmetric
        (list
          (handle->svg (conj path-id 0)
                       (:before selection)
                       position
                       (polar->cartesian [(- (::d/length vertex)) (::d/angle vertex)]))
          (handle->svg (conj path-id 1)
                       (:after selection)
                       position
                       (polar->cartesian [(::d/length vertex) (::d/angle vertex)])))

      :semi-symmetric
      (list
        (handle->svg (conj path-id 0)
                     (:before selection)
                     position
                     (polar->cartesian [(- (::d/before-length vertex)) (::d/angle vertex)]))
        (handle->svg (conj path-id 1)
                     (:after selection)
                     position
                     (polar->cartesian [(::d/after-length vertex) (::d/angle vertex)])))

      :asymmetric
      (list
        (handle->svg (conj path-id 0)
                     (:before selection)
                     position
                     (polar->cartesian [(- (::d/before-length vertex)) (::d/before-angle vertex)]))
        (handle->svg (conj path-id 1)
                     (:after selection)
                     position
                     (polar->cartesian [(::d/after-length vertex) (::d/after-angle vertex)]))))))


(defn- selection-for-anchor
  "Return which handles are selected for a given anchor.

  Given the vector of selected-handles and an anchor index, return the
  handles for the given anchor that are selected.

  For example, when anchor-index = 1,

    [[1 :before] [2 :after] [1 :after]] -> #{:before :after}
  "
  [anchor-index selected-handles]
  {:pre [(valid? int? anchor-index)
         (valid? (s/nilable ::d/selected-handles) selected-handles)]}

  (into #{}
    (map second
         (filter #(= anchor-index (first %)) selected-handles))))

(defn- node-selected?
  "Is the node, anchor or handle, with the given index selected in
   'selected-anchors'? 'selected-anchors' can be nil, in which case we return
   false
  "
  [anchor-index selected-nodes]
  {:pre [(valid? int? anchor-index)
         (valid? (s/nilable (s/or :all #{:all},
                                  :indicies (s/coll-of int? :kind vector?)))
                 selected-nodes)]
   ; Double check that return is definitely false if input is nil.
   :post [(if (nil? selected-nodes) (not %) true)]}

  (or (= :all selected-nodes)
      (value-in-collection? anchor-index selected-nodes)))

(defmethod object->svg :path [path] {:pre [(s/valid? ::d/path path)]}
  [:g {:id (::d/id path) :key (::d/id path)}
    [:path {:fill "none"
            :stroke "black"
            :d (str/join " "
                (concat
                  ; Move-to command to go to start of the path
                  [(position->svg (path-first-anchor path) "M")]
                  ; Map each pair of corners to a curve-to command
                  (map
                    #(vertex-pair->svg %)
                    (pairs (::d/vertices path)))))}]
    ; If we were asked to visualize any anchors then draw diamonds for them
    ; XXX This is a patten that we could refactor
    (keep-indexed
      (fn [i vertex]
        ; if 'i' is in 'display-anchors'
        (if (node-selected? i (::d/display-anchors path))
            (vertex-anchor->svg vertex
                                [(::d/id path) i]
                                (node-selected? i (::d/selected-anchors path))
                                (node-selected? i (::d/large-anchors path)))))
      (::d/vertices path))

    ; Same for handles
    (keep-indexed
      (fn [i vertex]
        (if (node-selected? i (::d/display-handles path))
          (vertex-handles->svg vertex
                               [(::d/id path) i]
                               (selection-for-anchor i (::d/selected-handles path)))))
      (::d/vertices path))])

;;; object->svg [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object->svg :circle [circle] {:pre [(valid? ::d/circle circle)]}
  (let [{centre ::d/position, radial ::d/radial, id ::d/id} circle]

    [:g {:id id :key id}
      [:circle {:fill "none"
                :stroke "black"
                :cx (centre 0)
                :cy (centre 1)
                :r (vector-length radial)}]

      (if (node-selected? 0 (::d/display-anchors circle))
        (vertex-anchor->svg
          {::d/vertex-type :no-handles ::d/position centre}
          [id 0]
          (node-selected? 0 (::d/selected-anchors circle))
          (node-selected? 0 (::d/large-anchors circle))))

      (if (node-selected? 1 (::d/display-anchors circle))
        (vertex-anchor->svg
          {::d/vertex-type :no-handles ::d/position (pos-add centre radial)}
          [id 1]
          (node-selected? 1 (::d/selected-anchors circle))
          (node-selected? 1 (::d/large-anchors circle))))]))

;;; object-with-node-moved ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-methods that know how to move the anchors/handles (dit "nodes") of
;; the different objects.

(defmulti
  object-with-node-moved
  ; Dispatch function
  (fn
    [obj]
    {:pre [(s/valid? ::d/object obj)]}
    (::d/object-type obj)))

;;; object-with-node-moved [PATH] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-transformed-after-handle
  "Apply a Cartesian transform to an after-handle"
  [vertex len-kw angle-kw transform]
  ; 1) Convert to cartesian
  ; 2) apply transform
  ; 30 Convert back to polar
  (let [[r t]
        (-> [(len-kw vertex) (angle-kw vertex)]
            polar->cartesian
            transform
            cartesian->polar)]
    (assoc (assoc vertex len-kw r) angle-kw t)))

(defn- with-transformed-before-handle
  "Apply a Cartesian transform to an before-handle"
  [vertex len-kw angle-kw transform]
  ; Same as above, but we have to mirror the coordinate system.
  (let [[r t]
        (-> [(len-kw vertex) (angle-kw vertex)]
            polar->cartesian
            pos-diff   ; With 1 arg this negates
            transform
            pos-diff
            cartesian->polar)]
    (assoc (assoc vertex len-kw r) angle-kw t)))


(defmulti ^{:private true} vertex-with-handle-moved
  "Return a copy of the vertex with the handle moved"
  (fn [vertex]
    {:pre [(s/valid? ::d/vertex vertex)]}
    (::d/vertex-type vertex)))

(defmethod vertex-with-handle-moved :no-handles
  [vertex handle-id transform]
  (log/warn "Path ignoring invalid node ID (3)"))

(defmethod vertex-with-handle-moved :handle-before
  [vertex handle-id transform]
  (if (not= handle-id 0)
      (log/warn "Path ignoring invalid node ID (4)")
      (with-transformed-before-handle vertex ::d/before-length ::d/before-angle transform)))
      ;; XXX Need to negate position

(defmethod vertex-with-handle-moved :handle-after
  [vertex handle-id transform]
  (if (not= handle-id 1)
      (log/warn "Path ignoring invalid node ID (4)")
      (with-transformed-after-handle vertex ::d/after-length ::d/after-angle transform)))

(defmethod vertex-with-handle-moved :symmetric
  [vertex handle-id transform]
  (if (= handle-id 0)
      (with-transformed-before-handle vertex ::d/length ::d/angle transform)
      (with-transformed-after-handle vertex ::d/length ::d/angle transform)))

(defmethod vertex-with-handle-moved :semi-symmetric
  [vertex handle-id transform]
  (if (= handle-id 0)
      (with-transformed-before-handle vertex ::d/before-length ::d/angle transform)
      (with-transformed-after-handle vertex ::d/after-length ::d/angle transform)))

(defmethod vertex-with-handle-moved :asymmetric
  [vertex handle-id transform]
  (if (= handle-id 0)
      (with-transformed-before-handle vertex ::d/before-length ::d/before-angle transform)
      (with-transformed-after-handle vertex ::d/after-length ::d/after-angle transform)))


;; This has to handle anchors and 'handles'.
(defmethod object-with-node-moved :path
  [path node-path transform]
  {:pre [(s/valid? ::d/path path)
         (s/valid? ::d/id-path node-path)]}

  (let [[anchor-id handle-id & rest] node-path]
    (cond
      rest            (log/warn "Path ignoring invalid node ID (1)")
      (not anchor-id) (log/warn "Path ignoring invalid node ID (2)")
      handle-id (update-in path [::d/vertices anchor-id] #(vertex-with-handle-moved % handle-id transform))
      anchor-id (update-in path [::d/vertices anchor-id ::d/position] transform))))

;;; object-with-node-moved [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-node-moved :circle
  [circle node-id transform]
  {:pre [(valid? ::d/circle circle)]}

  (let [anchor-id (first node-id)]
    (case anchor-id
      0 (update circle ::d/position transform)
      1 (update circle ::d/radial transform)
      (log/warn "Circle ignoring invalid node ID"))))


;;; moved-object multi-method ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; If we normalized all of our objects so that they had a ::d/position
;; key and all verticies were relative to that (like a circle) then we
;; could move them with a generic function and we wouldn't need this mult
(defmulti
  moved-object
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;;; moved-object [PATH] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This has to handle anchors and 'handles'.
(defmethod moved-object :path
  [path transform]
;  {:pre [(valid? ::d/path path)]  <-- these are ignored by defmethod
;   :post [(valid? ::d/path path)]]
  (let [transform-vertex-position (fn [v] (update v ::d/position transform))]
    (update path ::d/vertices #(mapv transform-vertex-position %))))

;;; moved-object [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod moved-object :circle
  [circle transform]
;  {:pre [(valid? ::d/circle circle)] <-- these are ignored by defmethod
;   :post [(valid? ::d/circle circle)]]
  (update circle ::d/position transform))


;;; object-with-node-removed ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-methods that know how to remove the anchors/handles (dit "nodes") of
;; the different objects.

(defmulti
  object-with-node-removed
  ; Dispatch function
  (fn
    [obj]
    {:pre [(s/valid? ::d/object obj)]}
    (::d/object-type obj)))

;;; object-with-node-removed [PATH] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- remove-path-anchor
  "Remoe an anchor from a path"
  [vertices anchor-id]
  ; Refuse to delete if there are only two vertices; a path with only one vertex
  ; is useles and hard to edit. The user should delete the object instead.
  (if (< (count vertices) 3) vertices
    (utils/remove-from-vector vertices anchor-id)))

(defn- remove-vertex-handle
  "Remoe a handle from a path"
  [vertex handle-id]
  {:pre [(valid? ::d/vertex vertex) (int? handle-id)]}

  ;0
  ; no-handles -> no-handles
  ; handle-before -> no-handles
  ; handle-after -> handle-after
  ; handle-symmetric -> handle-after
  ; handle-semi-symmetric -> handle-after
  ; handle-asymmetric -> handle-after

  ;1
  ; no-handles -> no-handles
  ; handle-before -> handle-before
  ; handle-after -> no-handles
  ; handle-symmetric -> handle-before
  ; handle-semi-symmetric -> handle-before
  ; handle-asymmetric -> handle-before
  (let [{position ::d/position
         angle ::d/angle
         before-angle ::d/before-angle
         after-angle ::d/after-angle
         length ::d/length
         before-length ::d/before-length
         after-length ::d/after-length
         vertex-type ::d/vertex-type} vertex]
    (case [handle-id vertex-type]
      [0 :no-handles]     vertex
      [0 :handle-before]  {::d/vertex-type :no-handles ::d/position position}
      [0 :handle-after]   vertex
      [0 :symmetric]      {::d/vertex-type :handle-after ::d/position position ::d/after-angle angle ::d/after-length length}
      [0 :semi-symmetric] {::d/vertex-type :handle-after ::d/position position ::d/after-angle angle ::d/after-length after-length}
      [0 :asymmetric]     {::d/vertex-type :handle-after ::d/position position ::d/after-angle after-angle ::d/after-length after-length}

      [1 :no-handles]     vertex
      [1 :handle-before]  vertex
      [1 :handle-after]   {::d/vertex-type :no-handles ::d/position position}
      [1 :symmetric]      {::d/vertex-type :handle-before ::d/position position ::d/before-angle angle ::d/before-length length}
      [1 :semi-symmetric] {::d/vertex-type :handle-before ::d/position position ::d/before-angle angle ::d/before-length before-length}
      [1 :asymmetric]     {::d/vertex-type :handle-before ::d/position position ::d/before-angle before-angle ::d/before-length before-length}
      vertex)))

(defmethod object-with-node-removed :path
  [path [anchor-id handle-id]]
;  {:pre [(valid? ::d/path path)]  <-- these are ignored by defmethod
;   :post [(valid? ::d/path path)]]

  (cond
    (nil? anchor-id) path ; do nothing
    (nil? handle-id) (update path ::d/vertices
                       remove-path-anchor anchor-id handle-id)
    :else            (update-in path [::d/vertices anchor-id]
                       remove-vertex-handle handle-id)))


;;; object-with-node-removed [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-node-removed :circle
  [circle node-id-path]
;  {:pre [(valid? ::d/circle circle)] <-- these are ignored by defmethod
;   :post [(valid? ::d/circle circle)]]

  ; Nothing to do; a circle has two nodes. Neither can be removed without
  ; destroying the circle
  circle)


;;; object-with-node-added ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-methods that know how to add anchors/handles (dit "nodes") to
;; the different objects.

(defmulti
  object-with-node-added
  ; Dispatch function
  (fn
    [obj]
    {:pre [(s/valid? ::d/object obj)]}
    (::d/object-type obj)))

;;; object-with-node-added [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- vertex-pair->bezier-curve
  "Map a pair of vertices to four Bezier control points"
  [[start end]]
  {:pre [(valid? ::d/vertex start)
         (valid? ::d/vertex end)]
   :post [(valid? ::bezier/cubic-bezier-curve %)]}

  (let [start-after-length (::d/length start (::d/after-length start 0))
        start-after-angle (::d/angle start (::d/after-angle start 0))
        end-before-length (::d/length end (::d/before-length end 0))
        end-before-angle (::d/angle end (::d/before-angle end 0))
        first-point (::d/position start)
        last-point (::d/position start)]

    [first-point
     ; Second control point is absolute position of after handle of 'start'
     (utils/pos-add
       first-point
       (polar->cartesian [start-after-length start-after-angle]))
     ; Third control point is absolute position of before handle of end
     (utils/pos-add
       last-point
       (polar->cartesian [(- end-before-length) end-before-angle]))
     last-point]))


(defn- path->bezier-curves
  [path]
  {:pre (valid? ::d/path path)
   :post [(valid?
            (s/coll-of ::bezier/cubic-bezier-curve)
            %)]}

  (map
    vertex-pair->bezier-curve
    (utils/pairs (::d/vertices path))))

(defn- position->segment-index
  "Find the segment that was clicked on"
  [path position]
  {:pre [(valid? ::d/path path)
         (valid? ::d/position position)]}

  (bezier/closest-bezier-curve-index
    (path->bezier-curves path)
    position))

;; Add an anchor to the path at the given position
(defmethod object-with-node-added :path
  [path position]
;  {:pre [(valid? ::d/path path)]  <-- these are ignored by defmethod]
;   :post [(valid? ::d/path path)]]

  (let [vertex-index (position->segment-index path position)]
    (update path ::d/vertices
      utils/insert-into-vector
      (inc vertex-index)
      {::d/vertex-type :no-handles ::d/position position})))


;;; object-with-node-added [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Cannot add a node to a circle; so, this is a NOOP.
(defmethod object-with-node-added :circle [circle position] circle)



;;; object-with-node-selected ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-methods that know how to select anchors/handles (dit "nodes") to
;; the different objects.

(defmulti
  object-with-node-selected
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;; object-with-node-selected [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-node-selected :path
  [path node-id]
  (let [anchor-id (first node-id)]
    (-> path
      (assoc ::d/selected-anchors [anchor-id])
      (assoc ::d/selected-handles
        [[anchor-id :before]
         [anchor-id :after]]))))


;; object-with-node-selected [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-node-selected :circle
  [path node-id]
  (let [anchor-id (first node-id)]
    (-> path
      (assoc ::d/selected-anchors [anchor-id]))))


;;; object-with-vertex-type-set ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Change a vertex type.

(defmulti
  object-with-vertex-type-set
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;; object-with-node-selected [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-vertex-type-set :path
  [path vertex-index vertex-type]

  (let [vertex (get-in path [::d/vertices vertex-index])
        position (::d/position vertex)
        after-length (::d/length vertex (::d/after-length vertex 20))
        before-length (::d/length vertex (::d/before-length vertex 20))
        after-angle (::d/angle vertex (::d/after-angle vertex 20))
        before-angle (::d/angle vertex (::d/before-angle vertex 20))]

    (update path ::d/vertices
      assoc    ; <- Use assoc to replace an element in the vector
      vertex-index
      (case vertex-type
        :no-handles      {::d/vertex-type vertex-type ::d/position position}
        :handle-before   {::d/vertex-type vertex-type ::d/position position ::d/before-angle before-angle ::d/before-length before-length}
        :handle-after    {::d/vertex-type vertex-type ::d/position position ::d/after-angle after-angle ::d/after-length after-length}
        :symmetric       {::d/vertex-type vertex-type ::d/position position ::d/angle after-angle ::d/length after-length}
        :semi-symmetric  {::d/vertex-type vertex-type ::d/position position ::d/angle after-angle ::d/before-length before-length ::d/after-length after-length}
        :asymmetric      {::d/vertex-type vertex-type ::d/position position ::d/before-angle before-angle ::d/before-length before-length ::d/after-angle after-angle ::d/after-length after-length}
        {::d/vertex-type :no-handles ::d/position position}))))


;; object-with-node-selected [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This is a NOOP for circle
(defmethod object-with-vertex-type-set :circle
  [circle node-id vertex-type]
  circle)



;;; object-with-selected-vertex-type-set ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Change a vertex type.

(defmulti
  object-with-selected-vertex-type-set
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;; object-with-node-selected [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-selected-vertex-type-set :path
  [path vertex-type]

  (let [selected-anchors (::d/selected-anchors path)
        setter (fn setter [path_ vertex-index]
                 (object-with-vertex-type-set path_ vertex-index vertex-type))]
    (reduce setter path selected-anchors)))

;; object-with-node-selected [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-selected-vertex-type-set :circle
  [circle vertex-type] circle)  ; NOOP


;;; object-with-large-anchors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Multi-methods that know how to transform anchors in response to hover events

(defmulti
  object-with-large-anchors
  ; Dispatch function
  (fn
    [obj]
    {:pre [(valid? ::d/object obj)]}
    (::d/object-type obj)))

;; object-with-node-selected [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-large-anchors :path
  [path large-anchors]
  (assoc path ::d/large-anchors large-anchors))

;; object-with-node-selected [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-large-anchors :circle
  [circle large-anchors]
  ;; XXX Set this to update instead of assoc clojure.spec will catch the error.
  (assoc circle ::d/large-anchors large-anchors))
