;;
;; Bezier related functions
;;
(ns parabola.bezier
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [parabola.log :as log]
            [parabola.utils :refer [valid? pos-diff pos-add vector-length]]))

; An [x y] position
(s/def ::position (s/coll-of number? :kind vector :count 2))

; Four points which define a cubic Bezier curve
(s/def ::cubic-bezier-curve (s/coll-of ::position :count 4))

(defn bernstein3_0
  "https://fr.wikipedia.org/wiki/Polynôme_de_Bernstein"
  [t]
  (let [x (- 1 t)]
    (* x x x)))

(defn bernstein3_1
  "https://fr.wikipedia.org/wiki/Polynôme_de_Bernstein"
  [t]
  (let [x (- 1 t)]
    (* 3 t x x)))

(defn bernstein3_2
  "https://fr.wikipedia.org/wiki/Polynôme_de_Bernstein"
  [t]
  (* 3 t t (- 1 t)))

(defn bernstein3_3
  "https://fr.wikipedia.org/wiki/Polynôme_de_Bernstein"
  [t]
  (* t t t))

(defn scale-pos [f pos]
  {:pre [(valid? number? f)
         (valid? ::position pos)]}

  (mapv (partial * f) pos))

(defn cubic-bezier-point
  [[p0 p1 p2 p3] t]

  (pos-add (scale-pos (bernstein3_0 t) p0)
           (scale-pos (bernstein3_1 t) p1)
           (scale-pos (bernstein3_2 t) p2)
           (scale-pos (bernstein3_3 t) p3)))

(defn cubic-bezier-sample
  "n samples from the bubic Bezier curves with the given control points"
  [points n]
  {:pre [(valid? ::cubic-bezier-curve points)
         (valid? int? n)]}

  (map
    #(cubic-bezier-point points %)
    (range 0 1 (/ 1 n))))

(defn distance-to-curve [point curve]
  (apply
    min
    (map  ; <- map samples from curve to distances from point
      #(vector-length (pos-diff point %))
      (cubic-bezier-sample curve 40))))

(defn closest-bezier-curve-index
  "Given a collection of Beier curves, find the curve which passes closest to
   the given point"
  [curves point]
  {:pre [(valid? (s/coll-of ::cubic-bezier-curve) curves)
         (valid? ::position point)]}

  (first ; [index curve] -> index
    (apply
      min-key
      ; Criteria to minimize
      (fn [[i curve]] (distance-to-curve point curve))
      ; [curve1 curve1] -> [[0 curve] [1 curve]]
      (map-indexed vector curves))))
