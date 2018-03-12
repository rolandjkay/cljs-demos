(ns parabola.subs
  (:require [re-frame.core :as re-frame]
            [parabola.domain :as d]
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]))

(re-frame/reg-sub
  ::active-panel
  (fn [db _]
    (:active-panel db)))

; Yield true is any node is selected
(re-frame/reg-sub :subs/node-selected?
  (fn subs-node-selected [db _]
    (some
      ; Returns nil (false) is ::d/selected-anchors missing or referes to empty
      ; vector.
      (fn [[id obj]] (first (::d/selected-anchors obj)))
      (::d/objects db))))

;; Simple lookups from the db
;(trace-forms {:tracer (tracer :color "green")}
(re-frame/reg-sub :subs/selected-tool  (fn selected-tool-accessor [db _] (::d/selected-tool db)))
(re-frame/reg-sub :subs/objects (fn objects-accessor [db _] (vals (::d/objects db))))
