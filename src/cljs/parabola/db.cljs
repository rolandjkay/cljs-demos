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
                   {::vertex-type :handle-after ::position [25 50] ::angle 90 ::length 100}
                   {::vertex-type :symmetric ::position [75 100] ::angle 90 ::length 50}
                   {::vertex-type :handle-before ::position [150 75] ::angle 210 ::length 56}]}]

    ::d/selected-tool :move
    ::d/move-tool-state ::none})
