;; Schema for 'db', the application state
;;
(ns parabola.domain
  (:require [clojure.spec.alpha :as s]))

(s/def ::position (s/cat :x int? :y int?))
(s/def ::id int?)

;;; PATH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; There are three different point types for a path: sharp corner (no handles),
;; symmetric corner (one handle; other is mirror image); asymmetric corner
;; (two handles).

(s/def ::sharp-corner (s/cat :anchor ::position))
(s/def ::symmetric-corner  (s/cat :anchor ::position :handle ::position))
(s/def ::asymmetric-corner (s/cat :anchor ::position :handle-one ::position :handle-two ::position))

;; s/or is, effectively, a pattern match. It will return a vector in which the
;; first element gives the label of the spec that matches; e.g
;;
;; [:asymmetric-corner
;;  {:handle-two {:y 300, :x 300},
;;   :handle-one {:y 200, :x 200},
;;   :anchor {:y 100, :x 100})
(s/def ::corner (s/or :sharp-corner ::sharp-corner
                      :symmetric-corner ::symmetric-corner
                      :asymmetric-corner ::asymmetric-corner))
(s/def ::corners (s/coll-of ::corner :kind vector?))

;(s/def ::path (s/cat :obj-type #{:path}
;                     :id int?
;                     :corners (s/coll-of ::corner :kind vector?)))
(s/def ::path (s/keys :req [::object-type ::id ::corners]))

;;; CIRCLE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(s/def ::circle (s/cat :obj-type #{:circle}
;                       :id int?
;                       :radius int?))
(s/def ::radius int?)
(s/def ::circle (s/keys :req [::object-type ::id ::position ::radius]))

;;; OBJECT ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; One of the above objects
;; See https://clojure.org/guides/spec#_multi_spec

(s/def ::object-type keyword?)
;(s/def ::object (s/or :path ::path))

(defmulti object-type ::object-type)
(defmethod object-type :path [_] ::path)
(defmethod object-type :circle [_] ::circle)

(s/def ::object (s/multi-spec object-type ::object-type))

(s/def ::objects (s/coll-of ::object :kind vector?))

;;; TOOL STATES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::null-move-tool-state (s/or :none #{::none}
                                    :object (s/cat :object-type   #{::path}
                                                   :object-id     ::id
                                                   :origin        ::position
                                                   :client-origin ::position)))

(s/def ::db (s/keys :req [ ::objects
                           ::selected-tool
                           ::move-tool-state]))
