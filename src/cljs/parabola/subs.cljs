(ns parabola.subs
  (:require [re-frame.core :as re-frame]
            [parabola.domain :as d]
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]))

(re-frame/reg-sub
  ::active-panel
  (fn [db _]
    (:active-panel db)))


;(re-frame/reg-sub
; :selected-object
; (fn [db _]
;   (:selected-object db)))

;; Simple lookups from the db
(trace-forms {:tracer (tracer :color "green")}
  (re-frame/reg-sub :subs/selected-tool  (fn selected-tool-accessor [db _] (::d/selected-tool db)))
  (re-frame/reg-sub :subs/objects (fn objects-accessor [db _] (::d/objects db))))
