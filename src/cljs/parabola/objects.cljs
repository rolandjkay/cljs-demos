;;
;; SVG renderable objects
;;
(ns parabola.objects
  (:require [parabola.domain :as d]
            [parabola.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

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
  (let [ [[x y] & rest] (::d/corners path)]
    [x y]))

(assert (= (path-first-anchor {::d/object-type :path
                               ::d/id          0
                               ::d/corners     [[100 101 202 203] [303 404]]})
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

(defn- corner-pair->svg
  "Render a pair of corners to an SVG path line segment"
  [[start end]]
  {:pre [(s/valid? ::d/corner start)
         (s/valid? ::d/corner end)]}
  ; sax  -> start anchor X
  ; sh1x -> start handle one X
  (let [[sax say sh1x sh1y sh2x sh2y] start
        [eax eay eh1x eh1y eh2x eh2y] end
        [start-type _]                (s/conform ::d/corner start)
        [end-type _]                  (s/conform ::d/corner end)]

    (case [start-type end-type]
      ; Two sharp corners are linked by a line
      [:sharp-corner      :sharp-corner]
      (position->svg end "L")

      ; curve-to with the first handle at the anchor
      [:sharp-corner      :symmetric-corner]
      (str/join " " [(position->svg start "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])

      ; Same as above (XXX could we not copy-paste?))
      [:sharp-corner      :asymmetric-corner]
      (str/join " " [(position->svg start "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])

      ; symmetric curve-to with the handle at the last anchor
      [:symmetric-corner  :sharp-corner]
      (str/join " " [(position->svg end "S") (position->svg end)])

      [:symmetric-corner  :symmetric-corner]
      (str/join " " [(position->svg [eh1x eh1y] "S") (position->svg [eax eay])])

      [:symmetric-corner  :asymmetric-corner]
      (str/join " " [(position->svg [eh1x eh1y] "S") (position->svg [eax eay])])

      [:asymmetric-corner :sharp-corner]
      (str/join " " [(position->svg [sh1x sh1y] "C") (position->svg [eax eay]) (position->svg [eax eay])])

      [:asymmetric-corner :symmetric-corner]
      (str/join " " [(position->svg [sh2x sh2y] "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])])

      [:asymmetric-corner :asymmetric-corner]
      (str/join " " [(position->svg [sh2x sh2y] "C") (position->svg [eh1x eh1y]) (position->svg [eax eay])]))))

(defn- anchor->svg
  "Render an anchor"
  {:pre [(s/valid? ::d/anchor anchor)]}
  [[x y _ _ _ _ :as anchor] id]
  [:g {:key (str "a-" id)}
    [:polygon {:fill "black"
               :points (str (- x 3) "," y " "
                            x "," (+ y 3) " "
                            (+ x 3) "," y " "
                            x "," (- y 3) " "
                            (- x 3) "," y)}]])

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
                    #(corner-pair->svg %)
                    (u/pairs (::d/corners path)))))}]
    ; If we were asked to visualize any handles then draw diamonds for them
    ; - for on nil is safe.
    ;(for [anchor-index (::d/display-anchors path)]
    ;  (anchor->svg)]])
    (keep-indexed
      (fn [i corner]
        (if (some #(= i %) (::d/display-anchors path))
          (anchor->svg corner (str (::d/id path) "-" i))))
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
