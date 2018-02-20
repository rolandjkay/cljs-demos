;;
;; SVG renderable objects
;;
(ns parabola.objects
  (:require [parabola.domain :as d]
            [parabola.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(def ^{:private true} pos-diff (partial mapv -))
(def ^{:private true} pos-add (partial mapv +))

(defn- deg->rad
  "Convert an angle in degrees to radians"
  [t]
  (* 3.142 (/ t 180)))

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

(defn- position->svg
  "Render a position to an SVG string; [1 2]-> \"1,2\"

  Optionally takes an SVG command prefix; e.g.

  (position->svg [1 2] \"M\")
  ;; \"M1,2\"

  "
  ([position]
   (position->svg position ""))
  ([position cmd]
   {:pre [(s/valid? ::d/position position)
          (string? cmd)]}
   (str cmd (position 0) "," (position 1))))

;; This relies on the fact that the first two numbers in the first corner
;; is the anchor position, regardless of the corner type.
(defn- path-first-anchor
  "Map a path to its first point; useful for the initial move command"
  [path]
  {:pre [(s/valid? ::d/path path)]
   :post [(s/valid? ::d/position %)]}
  (::d/position (first (::d/vertices path))))

(assert (= (path-first-anchor {::d/object-type :path
                               ::d/id          0
                               ::d/vertices
                               [{::d/vertex-type :no-handles ::d/position [100 101]}
                                {::d/vertex-type :no-handles ::d/position [303 404]}]})
           [100 101]))

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
;  (let [{start-type          ::d/vertex-type
;         start-position      ::d/position
;         start-before-length ::d/before-length
;         start-after-length  ::d/after-length
;         start-before-angle  ::d/before-angle
;         start-after-angle   ::d/after-angle]
;        start
;        {end-type            ::d/vertex-type
;         end-position        ::d/position
;         end-before-length   ::d/before-length
;         end-after-length    ::d/after-length
;         end-before-angle    ::d/before-angle
;         end-after-angle     ::d/after-angle]
;        end
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

;(defn- xxxx [x]
;    (case [start-type end-type]
;      ; Vertices linked by a line
;      [:no-handles      :no-handles]
;      (position->svg end-position "L")
;
;      [:handle-before   :no-handles]
;      (position->svg end-position "L")
;
;      [:handle-after    :symmetric]
;      (do
;        (println "HHH" (pos-diff end-position start-position))
;        (s/assert number? start-after-length)
;        (s/assert number? start-after-angle)
;        (s/assert number? end-before-length)
;        (s/assert number? end-before-angle)
;        ; There is a handle for both ends of the segment. So, we need to use C
;        (str/join " " [(position->svg [(* start-after-length (js/Math.cos start-after-angle))
;                                       (* start-after-length (js/Math.sin start-after-angle))
;                                      "c"
;                       (position->svg [(* (- end-before-length) (js/Math.cos end-before-angle))
;                                       (* (- end-before-length) (js/Math.sin end-before-angle))
;                       (position->svg (pos-diff end-position start-position)))]
;
;      [:symmetric    :handle-before]
;      (do
;        (s/assert number? start-after-length)
;        (s/assert number? start-after-angle)
;        (s/assert number? end-before-length)
;        (s/assert number? end-before-angle)
;        ; There is a handle for both ends of the segment. So, we need to use C
;        (str/join " " [(position->svg (pos-add start-position (polar->cartesian [start-after-length start-after-angle]))
;                                      "C"
;                       (position->svg (polar->cartesian [end-after-length end-after-angle]))
;                       (position->svg (pos-diff end-position start-position)))]
;
;      [:handle-after    :handle-before]
;      (do
;        (s/assert number? start-after-length)
;        (s/assert number? start-after-angle)
;        (s/assert number? end-before-length)
;        (s/assert number? end-before-angle)
;        (println "HHHH" (pos-diff end-position start-position))
;        ; There is a handle for both ends of the segment. So, we need to use C
;        (str/join " " [(position->svg (pos-add start-position (polar->cartesian [start-after-length start-after-angle]))
;                                      "C"
;                       (position->svg (pos-add end-position (polar->cartesian [(- end-before-length) end-before-angle])))
;                       (position->svg end-position))])))
;
;    (case [start-type end-type]
;      ; Two sharp corners are linked by a line
;      [:sharp-corner      :sharp-corner]
;      (position->svg end "L")
;
;      ; curve-to with the first handle at the anchor
;      [:sharp-corner      :symmetric-corner]
;      (str/join " " [(position->svg start "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])
;
;      ; Same as above (XXX could we not copy-paste?))
;      [:sharp-corner      :asymmetric-corner]
;      (str/join " " [(position->svg start "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])
;
;      ; symmetric curve-to with the handle at the last anchor
;      [:symmetric-corner  :sharp-corner]
;      (str/join " " [(position->svg end "S") (position->svg end)])
;
;      [:symmetric-corner  :symmetric-corner]
;      (str/join " " [(position->svg [eh1x eh1y] "S") (position->svg [eax eay])])
;
;      [:symmetric-corner  :asymmetric-corner]
;      (str/join " " [(position->svg [eh1x eh1y] "S") (position->svg [eax eay])])
;
;      [:asymmetric-corner :sharp-corner]
;      (str/join " " [(position->svg [sh1x sh1y] "C") (position->svg [eax eay]) (position->svg [eax eay])])
;
;      [:asymmetric-corner :symmetric-corner]
;      (str/join " " [(position->svg [sh2x sh2y] "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])
;
;      [:asymmetric-corner :asymmetric-corner]
;      (str/join " " [(position->svg [sh2x sh2y] "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])]))))

(defn- corner-anchor->svg
  "Render an anchor"
  {:pre [(s/valid? ::d/corner corner)]}
  [[x y _ _ _ _ :as corner] id]
  [:g {:key (str "a-" id)}
    [:polygon {:fill "black"
               :points (str (- x 3) "," y " "
                            x "," (+ y 3) " "
                            (+ x 3) "," y " "
                            x "," (- y 3) " "
                            (- x 3) "," y)}]])

(defn- handle->svg
  "Render a single handle"
  [anchor-position handle-position id]
  {:pre [(s/valid? ::d/position anchor-position)
         (s/valid? ::d/position handle-position)]}

  [:g {:key (str "h-" id)}
    ;; Dashed line from path to handle
    [:path {:fill "none"
            :stroke "black"
            :stroke-dasharray "5,5"
            :d (str/join " "
                 [(position->svg anchor-position "M")
                  (position->svg handle-position "L")])}]

    ;; The handle
    [:circle {:cx (handle-position 0) :cy (handle-position 1) :r "5"
              :fill "black"}]])


(defn- corner-handles->svg
  "Render the handles of an anchor"
  {:pre [(s/valid? ::d/corner corner)]}
  [[x y h1x h1y h2x h2y :as corner] id]
  (list
    (if h1x (handle->svg [x y] [h1x h1y] (str id "-1")) nil)
    (if h2x (handle->svg [x y] [h2x h2y] (str id "-2")) nil)))

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
                    (u/pairs (::d/vertices path)))))}]
    ; If we were asked to visualize any anchors then draw diamonds for them
    ; XXX This is a patten that we could refactor
    (keep-indexed
      (fn [i corner]
        ; if 'i' is in 'display-anchors'
        (if (some #(= i %) (::d/display-anchors path))
          (corner-anchor->svg corner (str (::d/id path) "-" i))))
      (::d/corners path))

    ; Same for anchors
    (keep-indexed
      (fn [i corner]
        (if (some #(= i %) (::d/display-handles path))
          (corner-handles->svg corner (str (::d/id path) "-" i))))
      (::d/corners path))])

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
