(ns parabola.db
  (:require [parabola.domain :as d]))

(def default-db
  {
    ;; Used to ensure unique object IDs
    ::d/next-object-id 1,

    ::d/objects  {0 {::d/object-type :path
                     ::d/id          0
                     ::d/vertices
                     [
                      {::d/vertex-type :handle-after ::d/position [25 50] ::d/after-angle 90 ::d/after-length 100}
                      {::d/vertex-type :symmetric ::d/position [75 100] ::d/angle 90 ::d/length 50}
                      {::d/vertex-type :handle-before ::d/position [150 75] ::d/before-angle 210 ::d/before-length 56},]}}

    ::d/selected-tool :tools/node-move,

    ;; Any state needed by the tools
    :db/tool-state nil,})
