(ns parabola.domain
  (:require [clojure.spec.alpha :as s]))

(s/def ::position (s/cat :x int? :y int?))
(s/def ::anchor (s/cat :position ::position
                       :handle-one-position (s/? ::position)
                       :handle-two-position (s/? ::position)))

(s/def ::null-move-tool-state (s/or :none #{::none}
                                    :object (s/cat :object-type   #{::path}
                                                   :object-id     ::id
                                                   :origin        ::position
                                                   :client-origin ::position)))
