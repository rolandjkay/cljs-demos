(ns parabola.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 :anchor-points
 (fn [db _]
   (get-in db [:anchor-points])))

(re-frame/reg-sub
 :selected-object
 (fn [db _]
   (:selected-object db)))

(re-frame/reg-sub
 :handle-positions
 (fn [db _]
   (:handle-positions db)))
