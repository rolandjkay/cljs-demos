;;
;; Convertion of an objects map to a textual markup
;;
(ns parabola.ml
  (:require [clojure.spec.alpha :as s]
            [parabola.utils :as utils]
            [parabola.domain :as domain]))


;;; object->markup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti
  object->markup
  ; Dispatch function
  (fn
    [obj]
    {:pre [(utils/valid? ::domain/object obj)]}
    (::domain/object-type obj)))

;; object->markup [PATH]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- vertex->markup [vertex]
  {:pre [(utils/valid? ::domain/vertex vertex)]}

  (let [before-angle (::domain/before-angle vertex)
        before-length (::domain/before-length vertex)
        after-angle (::domain/after-angle vertex)
        after-length (::domain/after-length vertex)
        angle (::domain/angle vertex)
        length (::domain/length vertex)
        position (::domain/position vertex)
        vertex-type (::domain/vertex-type vertex)]

    [:vertex
      (case vertex-type
        :no-handles {:vertex-type "no-handles" :position position}
        :handle-before {:vertex-type "handle-before" :position position :angle before-angle :length before-length}
        :handle-after {:vertex-type "handle-after" :position position :angle after-angle :length after-length}
        :semi-symmetric {:vertex-type "semi-symmetric" :position position :angle angle :length-one before-length :length-two after-length}
        :symmetric {:vertex-type "symmetric" :position position :angle angle :length length}
        :asymmetric {:vertex-type "symmetric" :position position :first-angle before-angle :first-length before-length :second-angle after-angle :second-length after-length})]))

(defmethod object->markup :path [path]
  (into []
    (concat
      [:path {}]
      (mapv vertex->markup (::domain/vertices path)))))

;; object->markup [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- radial->radius [radial]
  {:pre [(utils/valid? ::domain/position radial)]}
  (js/Math.round (first (utils/cartesian->polar radial))))

(defmethod object->markup :circle [circle]
  [:circle
    {:position (::domain/position circle)
     :radius (radial->radius (::domain/radial circle))}])

;;; objects->markup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn objects->markup
  "Convert a map of objects to a str containing markup"
  [objects]
  {:pre (utils/valid? ::domain/objects objects)}
  (vec
    (concat
      [:objects {}]
      (map
        object->markup
        objects))))
