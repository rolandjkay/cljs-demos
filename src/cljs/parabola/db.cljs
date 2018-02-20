(ns parabola.db
  (:require [parabola.domain :as d]))

(def default-db-old
  {:name "re-frame"
   :anchor-points  [{:x 25 :y 50}
                    {:x 75 :y 100}
                    {:x 150 :y 75}]
   :handle-positions [ {:x 25 :y 150}
                       {:x 75 :y 150}
                       {:x 100 :y 25}]
   :selected-object [:object-type :none
                     :object-index :none
                     :origin nil
                     :client-origin nil]
   :selected-tool :drag})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-db
  {
    ::d/objects [
                 {::d/object-type :path
                  ::d/id          0
                  ::d/vertices
                  [
                   {::d/vertex-type :handle-after ::d/position [25 50] ::d/after-angle 90 ::d/after-length 100}
                   {::d/vertex-type :symmetric ::d/position [75 100] ::d/angle 90 ::d/length 50}
                   {::d/vertex-type :handle-before ::d/position [150 75] ::d/before-angle 210 ::d/before-length 56}]}]

    ::d/selected-tool :move
    ::d/move-tool-state ::none})
