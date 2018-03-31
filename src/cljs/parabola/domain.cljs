;; Schema for 'db', the application state
;;
(ns parabola.domain
  (:require [clojure.spec.alpha :as s]))

(s/def ::position (s/tuple number? number?))
(s/def ::id int?)

;; The IDs are concatonated together as string separated by '/' on the DOM.
(def dom-id-regex #"^[0-9](/[0-9])*$")
(s/def ::dom-id (s/and string? #(re-matches dom-id-regex %)))

;; For processing, we split IDs like "1/2/3" into [1 2 3]; we call this the
;; ID path, as it gives the path of the node relative to the document root
(s/def ::id-path (s/coll-of int?))

;;; PATH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; XXX Actually, I think it would be easier to hold the positions of the
;; handles as relative cartesian coordinates, rather than polar.
;; Difference types of vertex:
;; no-handles
;; handle-before                                         angle, length
;; handle-after                                          angle, length
;; two-handles-symmetric     (lengths and angles equal)  angle, length
;; two-handle-semi-symmetric (angles equal)              angle, length1, length2
;; two-handle-asymmetric                                 angle1, angle2, len x 2
(s/def ::vertex-no-handles (s/keys :req [::vertex-type ::position]))
(s/def ::vertex-with-handle-before (s/keys :req [::vertex-type ::position ::before-angle ::before-length]))
(s/def ::vertex-with-handle-after (s/keys :req [::vertex-type ::position ::after-angle ::after-length]))
(s/def ::vertex-symmetric (s/keys :req [::vertex-type ::position ::angle ::length]))
(s/def ::vertex-semi-symmetric (s/keys :req [::vertex-type ::position ::angle ::before-length ::after-length]))
(s/def ::vertex-asymmetric (s/keys :req [::vertex-type ::position ::before-angle ::after-angle ::before-length ::after-length]))

(s/def ::vertex-type keyword?)
(defmulti vertex-type ::vertex-type)
(defmethod vertex-type :no-handles [_] ::vertex-no-handles)
(defmethod vertex-type :handle-before [_] ::vertex-with-handle-before)
(defmethod vertex-type :handle-after [_] ::vertex-with-handle-after)
(defmethod vertex-type :symmetric [_] ::vertex-symmetric)
(defmethod vertex-type :semi-symmetric [_] ::vertex-semi-symmetric)
(defmethod vertex-type :asymmetric [_] ::vertex-asymmetric)

(s/def ::vertex (s/multi-spec vertex-type ::vertex-type))

(s/def ::vertices (s/coll-of ::vertex :kind vector?))

;; Vectors of the anchors/handles to visualize
;; - These contain a list of IDs which the render function is expected to
;;   display.
(s/def ::display-anchors (s/or
                           :all #{:all}
                           :indicies (s/coll-of int? :kind vector?)))

(s/def ::display-handles ::display-anchors)

(s/def ::handle-selector (s/tuple int? #{:before :after}))
(s/def ::selected-handles (s/coll-of ::handle-selector :kind vector?))
(s/def ::selected-anchors (s/coll-of int? :kind vector?))
(s/def ::large-anchors (s/coll-of int? :kind vector?))

;(s/def ::path (s/cat :obj-type #{:path}
;                     :id int?
;                     :corners (s/coll-of ::corner :kind vector?)))
(s/def ::path (s/keys :req [::object-type ::id ::vertices]
                      :opt [::display-anchors ::display-handles
                            ::selected-anchors ::selected-handles
                            ::large-anchors]))

;;; CIRCLE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::radial ::position)
(s/def ::circle (s/keys :req [::object-type ::id ::position ::radial]
                        :opt [::display-anchors ::selected-anchors ::large-anchors]))

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

(s/def ::objects (s/map-of int? ::object))

(s/def ::next-object-id ::id)

;;; TOOLS  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::selected-tool #{:tools/make-circle
                         :tools/make-path
                         :tools/node-move
                         :tools/node-delete
                         :tools/node-add
                         :tools/node-select
                         :tools/object-delete
                         :tools/object-move})

(s/def ::null-move-tool-state (s/or :none #{::none}
                                    :object (s/cat :object-type   #{::path}
                                                   :object-id     ::id
                                                   :origin        ::position
                                                   :client-origin ::position)))

(s/def ::db (s/keys :req [ ::next-object-id
                           ::objects
                           ::selected-tool]))
