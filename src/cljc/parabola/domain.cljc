;; Schema for 'db', the application state
;;
(ns parabola.domain
  (:require [clojure.spec.alpha :as s]))

(s/def ::position (s/cat :x int? :y int?))

;; There are three different point types for a path: sharp corner (no handles),
;; symmetric corner (one handle; other is mirror image); asymmetric corner
;; (two handles).

(s/def ::sharp-corner (s/cat :anchor ::position))
(s/def ::symmetric-corner  (s/cat :anchor ::position :handle ::position))
(s/def ::asymmetric-corner (s/cat :anchor ::position :handle-one ::position :handle-two ::position))
(s/def ::corner (s/or :sharp-corder ::sharp-corner
                      :symmetric-corner ::symmetric-corner
                      :asymmetric-corner ::asymmetric-corner))

(s/def ::path (s/cat :obj-type #{:path}
                     :id int?
                     :corners (s/coll-of ::corner :kind vector?)))

(s/def ::null-move-tool-state (s/or :none #{::none}
                                    :object (s/cat :object-type   #{::path}
                                                   :object-id     ::id
                                                   :origin        ::position
                                                   :client-origin ::position)))

(s/def ::object (s/or :path ::path))
(s/def ::objects (s/coll-of ::object :kind vector?))

(s/def ::db (s/keys :req [ ::objects
                           ::selected-tool
                           ::move-tool-state]))
