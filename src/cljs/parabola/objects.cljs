;;
;; SVG renderable objects
;;
(ns parabola.objects
  (:require [parabola.domain :as d]
            [parabola.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn position->svg
  "Render a position to an SVG string; [1 2]-> \"1,2\""
  [position]
  {:pre [(s/valid? ::d/position position)]}
  (str (position 0) "," (position 1)))

;; Do we really need this for dispatch???
(defn object-type [obj]
  (let [parsed (s/conform ::d/object obj)]
    (if (= parsed ::s/invalid)
        (throw (ex-info "Invalid object" (s/explain-data ::d/object obj)))
        (::d/object-type parsed))))

;(println (object-type  {::d/object-type :path ::d/id 0 ::d/corners []}))

(defmulti object->svg object-type) ;(fn [obj (first obj)]))

;;; PATH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn corner-pair->svg
  "Render a pair of corners to an SVG path line segment"
  [[start end]]
  {:pre [(s/valid? ::d/corner start)
         (s/valid? ::d/corner end)]}
  (let [[start-type start*] (s/conform ::d/corner start)
        [end-type   end*]   (s/conform ::d/corner end)]
    (case [start-type end-type]
      [:sharp-corner      :sharp-corner] (str "L" (position->svg end) " ")
      [:sharp-corner      :symmetric-corner] "C"
      [:sharp-corner      :asymmetric-corner] "C"
      [:symmetric-corner  :sharp-corner] "S"
      [:symmetric-corner  :symmetric-corner] "S"
      [:symmetric-corner  :asymmetric-corner] "S"
      [:asymmetric-corner :sharp-corner] "C"
      [:asymmetric-corner :symmetric-corner] "C"
      [:asymmetric-corner :asymmetric-corner] "C")))

(defmethod object->svg :path [path] {:pre [(s/valid? ::d/path path)]}
  (println)


  [:path {:id (::d/id path)
          :fill "none"
          :stroke "black"
          :d (str/trim
               (reduce
                 #(str %1 (corner-pair->svg %2))
                 ; Initial value of reduce is the move-to command
                 ; XXX This is ridiculous
                 (some-> path
                         ::d/corners
                         first                             ; Get first corner
                         ((partial take 2))                ; Get position
                         ((partial into (vector)))
                         position->svg
                         (#(str "M" % " ")))
                 ; Reduce over pairs of corners
                 (u/pairs (::d/corners path))))}])



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
