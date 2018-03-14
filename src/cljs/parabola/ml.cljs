;;
;; Convertion of an objects map to a textual markup
;;
(ns parabola.ml
  (:require [clojure.spec.alpha :as s]
            [parabola.utils :as utils]
            [parabola.domain :as domain]))

(defn position->markup
  "[100 200] -> \"100,200\" in purple"
  [position]
  {:pre [(utils/valid? ::domain/position position)]}

  [:span {:style {:color "purple"}}
    "\"" (position 0) "," (position 1) "\""])

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

(defn- vertex->markup [index vertex]
  {:pre [(utils/valid? int? index)
         (utils/valid? ::domain/vertex vertex)]}

  (let [before-angle (::domain/before-angle vertex)
        before-length (::domain/before-length vertex)
        after-angle (::domain/after-angle vertex)
        after-length (::domain/after-length vertex)
        angle (::domain/angle vertex)
        length (::domain/length vertex)
        position (::domain/position vertex)
        vertex-type (::domain/vertex-type vertex)]

    [:span {:key index, :style {:color "cyan"}}
      "<vertex position=" (position->markup position)
      (case vertex-type
        :no-handles (str "vertex-type=\"no-handles\"")
        :handle-before (str "vertex-type=\"handle-before\" angle=\"" before-angle "\" length=\"" before-length "\"")
        :handle-after (str "vertex-type=\"handle-after\" angle=\"" after-angle "\" length=" after-length)
        :semi-symmetric (str "vertex-type=\"semi-symmetric\" angle=" angle " length-one=" before-length " length-two=" after-length)
        :symmetric (str "vertex-type=\"symmetric\" angle=" angle " length=" length)
        :asymmetric (str "vertex-type=\"symmetric\" first-angle=" before-angle " first-length=" before-length  " second-angle=" after-angle " second-length=" after-length))

      "/>"
      [:br]]))


(defmethod object->markup :path [path]
  [:span {:key (::domain/id path), :style {:color "cyan"}}
    "<path>" [:br]
    (map-indexed vertex->markup (::domain/vertices path))
    "</path>"
    [:br]])

;; object->markup [CIRCLE]) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- radial->radius [radial]
  {:pre [(utils/valid? ::domain/position radial)]}
  (js/Math.round (first (utils/cartesian->polar radial))))

(defmethod object->markup :circle [circle]
  [:span {:key (::domain/id circle), :style {:color "cyan"}}
    "<circle "
      [:span {:style {:color "green"}} "position"]
      [:span {:style {:color "white"}} "="]
      (position->markup (::domain/position circle))
      [:span {:style {:color "green"}} " radius"]
      [:span {:style {:color "white"}} "="]
      [:span {:style {:color "purple"}}
        "\""
        (radial->radius (::domain/radial circle))
        "\""]
    "/>"
    [:br]])


;;; objects->markup ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn objects->markup
  "Convert a map of objects to a str containing markup"
  [objects]
  {:pre (utils/valid? ::domain/objects objects)}
  (map
    object->markup
    objects))
