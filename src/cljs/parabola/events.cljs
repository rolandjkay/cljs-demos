(ns parabola.events
  (:require [re-frame.core :as re-frame]
            [parabola.db :as db]
            [parabola.domain :as d]
            [parabola.tools :as tools]
            [clairvoyant.core :refer-macros [trace-forms]]
            [re-frame-tracer.core :refer [tracer]]))



(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))


(re-frame/reg-event-db
  :cmds/select-tool
  (fn [db [_ tool-kw]]
    (assoc db ::d/selected-tool tool-kw)))


;;
;; Forward any interactions with the 'canvas' to the selected tool.

(trace-forms {:tracer (tracer :color "red")}
  (re-frame/reg-fx
    :tools/dispatch-click
    (fn tools-display-click-fx-handler [[tool-kw position]]
      (some-> tool-kw tools/tools-map (#(tools/click % position)))))

  (re-frame/reg-event-fx
    :canvas/click
    (fn canvas-click-handler [cofx [_ position]]
        {:tools/dispatch-click [(get-in cofx [:db ::d/selected-tool]) position]})))


;;
;; Object creation events


(trace-forms {:tracer (tracer :color "red")}
  ;; XXX This is, basically, a ctor for circle.
  (re-frame/reg-event-db
    :objects/create-circle
    (fn create-circle-handler [db [_ position radius]]
      (update db ::d/objects conj {::d/object-type :circle,
                                   ::d/id (count (::d/objects db)),
                                   ::d/position position,
                                   ::d/radial [radius 0]}))))
