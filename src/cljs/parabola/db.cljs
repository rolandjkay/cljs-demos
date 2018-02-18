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
                  ::d/corners     [
                                   [25 50, 25 150]
                                   [75 100, 75 150]
                                   [150 75, 100 25]]}]

    ::d/selected-tool :move
    ::d/move-tool-state ::none})
