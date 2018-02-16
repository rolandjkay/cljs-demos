(ns parabola.events
  (:require [re-frame.core :as re-frame]
            [parabola.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
  :set-start-point-x
  (fn [db [_ val]]
    (assoc-in db [:anchor-points 0 :x] val)))
    ;(assoc db :start-point (assoc (:start-point db) :x val))))

(re-frame/reg-event-db
  :select-object
  (fn [db [_ object-type object-index client-origin]]
    (assoc db :selected-object
      {
        :object-type object-type
        :object-index object-index
        :client-origin client-origin
        :origin (get-in db [(if (= object-type :handle) :handle-positions :anchor-points) object-index])})))


(defn neg-position [position]
  ; Apply '-' to all values
  (reduce (fn [m [k v]] (assoc m k (- v))) {} position))

(defn add-positions [& positions]
  {:x (apply + (map :x positions))
   :y (apply + (map :y positions))})

(re-frame/reg-event-db
  :mouse-move
  (fn [db [_ client-position]]
    ; If the selected handle is one of the handles (and not :none) then
    ; update that handles position.
    (let [{:keys [object-type object-index client-origin origin]}  (:selected-object db)]
      (cond (= object-type :handle)
            (assoc-in db [:handle-positions object-index]
              (add-positions origin client-position (neg-position client-origin)))

            (= object-type :anchor)
            (assoc-in db [:anchor-points object-index]
              (add-positions origin client-position (neg-position client-origin)))

            :else db))))

(re-frame/reg-event-db
  :add-anchor
  (fn [db [_]]
    (assoc db :anchor-points (conj (:anchor-points db) {:x 50 :y 50})
              :handle-positions (conj (:handle-positions db) {:x 100 :y 50}))))


(re-frame/reg-event-db
  :select-delete-anchor-tool
  (fn [db [_]]
    (assoc db :selected-tool :delete)))
