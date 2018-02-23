;;
;; SVG renderable objects
;;
(ns parabola.objects
  (:require [parabola.domain :as d]
            [parabola.utils :refer [value-in-collection? pairs pos-add pos-diff split-id]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [adzerk.cljs-console :as log :include-macros true]))

(defn- deg->rad
  "Convert an angle in degrees to radians"
  [t]
  (* (.-PI js/Math) (/ t 180)))

(defn- rad->deg
  "Convert an angle in radians to degrees"
  [t]
  (* 180 (/ t (.-PI js/Math))))

(defn- polar->cartesian
  "Convert a polar coordinate to a castesian one"
  [[r t]]
  {:pre [(s/valid? ::d/position [r t])] :post [(s/valid? ::d/position %)]}
  [(* r (js/Math.cos (deg->rad t)))
   (* r (js/Math.sin (deg->rad t)))])

(s/fdef polar->cartesian
  :args (s/and :polar ::d/position
               #(>= ((-> % :polar 0) 0)))
  :ret ::d/position)

(defn- cartesian->polar
  "Convert a Cartesian coordinate to a polar one"
  [[x y]]
  {:pre [(s/valid? ::d/position [x y])] :post [(s/valid? ::d/position %)]}

  [  (js/Math.sqrt (+ (* x x) (* y y))),
     (+ (-> (/ y x) js/Math.atan rad->deg)
        (if (>= x 0)
            (if (>= y 0) 0  360)
            180))])


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
    {:pre [(s/valid? ::d/object obj)]}
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
  [vertex id selected]
  {:pre [(s/valid? ::d/vertex vertex)
         (s/valid? boolean? selected)]}

  (let [[x y] (::d/position vertex)]
    [:g.drag-me {:key (str id) :id (str id)}
      [:polygon {:fill  (if selected "green" "black")
                 :points (str (- x 3) "," y " "
                              x "," (+ y 3) " "
                              (+ x 3) "," y " "
                              x "," (- y 3) " "
                              (- x 3) "," y)}]]))

(defn- handle->svg
  "Render a single handle"
  [id selected anchor-position relative-handle-position]
  {:pre [(s/valid? ::d/position anchor-position)
         (s/valid? ::d/position relative-handle-position)]}
  (let [handle-position (pos-add anchor-position relative-handle-position)]
    [:g.drag-me {:key (str id) :id (str id)}
      ;; Dashed line from path to handle
      [:path {:fill "none"
              :stroke "black"
              :stroke-dasharray "5,5"
              :d (str/join " "
                   [(position->svg anchor-position "M")
                    (position->svg handle-position "L")])}]

      ;; The handle
      [:circle {:cx (handle-position 0) :cy (handle-position 1) :r "5"
                :fill (if selected "green" "black")}]]))


(defn- vertex-handles->svg
  "Render the handles of a vertex"
  {:pre [(s/valid? ::d/vertex vertex)
         (s/valid? (s/coll-f #{:before :after} :kind set?) selection)]}
  [vertex id selection]
  (let [position (::d/position vertex)
        type  (::d/vertex-type vertex)]
    (case type
      :no-handles nil
      :handle-before
        (handle->svg (str id "/" 0)
                     (:before selection)
                     position
                     (polar->cartesian [(- (::d/before-length vertex)) (::d/before-angle vertex)]))

      :handle-after
        (handle->svg (str id "/" 1)
                     (:after selection)
                     position
                     (polar->cartesian [(::d/after-length vertex) (::d/after-angle vertex)]))

      :symmetric
        (list
          (handle->svg (str id "/" 0)
                       (:before selection)
                       position
                       (polar->cartesian [(::d/length vertex) (::d/angle vertex)]))
          (handle->svg (str id "/" 1)
                       position
                       (:after selection)
                       (polar->cartesian [(- (::d/length vertex)) (::d/angle vertex)])))

      :asymmetric
        (list
          (handle->svg (str id "/" 0)
                       (:before selection)
                       position
                       (polar->cartesian [(- (::d/before-length vertex)) (::d/before-angle vertex)]))
          (handle->svg (str id "/" 1)
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
  {:pre [(s/valid? int? handle-index)
         (s/valid? ::d/selected-handles selected-handles)]}
  [anchor-index selected-handles]
  (into #{}
    (map second
         (filter #(= anchor-index (first %)) selected-handles))))

(defmethod object->svg :path [path] {:pre [(s/valid? ::d/path path)]}
  [:g {:id (::d/id path)}
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
        (if (value-in-collection? i (::d/display-anchors path))
            (vertex-anchor->svg vertex
                                (str (::d/id path) "/" i)
                                (value-in-collection? i (::d/selected-anchors path)))))
      (::d/vertices path))

    ; Same for handles
    (keep-indexed
      (fn [i vertex]
        (if (value-in-collection? i (::d/display-handles path))
          (vertex-handles->svg vertex
                               (str (::d/id path) "/" i)
                               (selection-for-anchor i (::d/selected-handles path)))))
      (::d/vertices path))])

;;; object->svg [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object->svg :circle [obj]
  (let [parsed (s/conform ::d/circle obj)]
    (if (= parsed ::s/invalid)
        (throw (ex-info "Invalid circle" (s/explain-data ::d/circle obj)))
        [:circle {:id (::d/id parsed)
                  :fill "none"
                  :stroke "black"
                  :cx (str (get-in parsed [::d/position :x]))
                  :cy (str (get-in parsed [::d/position :y]))
                  :r (str (::d/radius parsed))}])))



;;; move-anchors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
    (print [r t] (transform [r t]))
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
  (println "Path ignoring invalid node ID (3)"))

(defmethod vertex-with-handle-moved :handle-before
  [vertex handle-id transform]
  (if (not= handle-id 0)
      (println "Path ignoring invalid node ID (4)")
      (with-transformed-before-handle vertex ::d/before-length ::d/before-angle transform)))
      ;; XXX Need to negate position

(defmethod vertex-with-handle-moved :handle-after
  [vertex handle-id transform]
  (if (not= handle-id 1)
      (println "Path ignoring invalid node ID (4)")
      (with-transformed-after-handle vertex ::d/after-length ::d/after-angle transform)))

(defmethod vertex-with-handle-moved :symmetric
  [vertex handle-id transform]
  (if (= handle-id 0)
      (with-transformed-before-handle vertex ::d/length ::d/angle transform)
      (with-transformed-after-handle vertex ::d/length ::d/angle transform)))

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
      rest            (println "Path ignoring invalid node ID (1)")
      (not anchor-id) (println "Path ignoring invalid node ID (2)")
      handle-id (update-in path [::d/vertices anchor-id] #(vertex-with-handle-moved % handle-id transform))
      anchor-id (update-in path [::d/vertices anchor-id ::d/position] transform))))

;;; object->svg [CIRCLE] ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod object-with-node-moved :circle
  [circle node-id transform]
  {:pre [(s/valid? ::d/circle circle)]}

  (update circle ::d/position transform))
