(ns parabola.events
  (:require [re-frame.core :as re-frame]
            [parabola.db :as db]
            [parabola.domain :as d]
            [parabola.tools :as tools]
            [parabola.utils :as utils]
            [parabola.objects :as objects]
            [parabola.utils :refer [valid?]]
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

;; Helper which takes the db as last arg; useful with threading macro
(defn- select-tool [tool-kw db] (assoc db ::d/selected-tool tool-kw))

(trace-forms {:tracer (tracer :color "red")}
  (re-frame/reg-event-db
    :cmds/select-tool
    (cljs.core/fn [db [_ new-tool-kw]]  {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}
      (let [current-tool  (-> db ::d/selected-tool tools/tools-map)
            new-tool (new-tool-kw tools/tools-map)]
        (->> db
          (tools/on-unselected current-tool)
          (tools/on-selected new-tool)
          (select-tool new-tool-kw))))))

(trace-forms {:tracer (tracer :color "red")}
  (re-frame/reg-event-db
    :cmds/set-vertex-type
    (cljs.core/fn
      vertex-type-setter
      [db [_ vertex-type]]
      {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}
      ; Change all selected vertecies to the give type
      (update db ::d/objects
        #(utils/map-function-on-map-vals %
          (fn [obj] (objects/object-with-selected-vertex-type-set obj vertex-type)))))))

;;
;; Forward any interactions with the 'canvas' to the selected tool.

(re-frame/reg-event-db
  :canvas/click
  (cljs.core/fn [db [_ position id-path]] {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}
    ;; Let the currently-selected tool handle.
    (let [tool-kw (::d/selected-tool db),
          tool  (tool-kw tools/tools-map)]
      (tools/on-click tool db position id-path))))

(re-frame/reg-event-db
  :canvas/double-click
  (cljs.core/fn [db [_ position obj-id]] {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}
    ;; Let the currently-selected tool handle.
    (let [tool-kw (::d/selected-tool db),
          tool  (tool-kw tools/tools-map)]
      (tools/on-double-click tool db position obj-id))))

(re-frame/reg-event-db
  :canvas/move
  (cljs.core/fn
    [db [_ position id-path]]
    {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}

    ;; Let the currently-selected tool handle.
    (let [tool-kw (::d/selected-tool db),
          tool  (tool-kw tools/tools-map)]
      (tools/on-move tool db position id-path))))

(re-frame/reg-event-db
  :canvas/drag
  (cljs.core/fn [db [_ dpos obj-id]] {:pre [(valid? ::d/db db)], :post [(valid? ::d/db %)]}
    ;; Let the currently-selected tool handle.
    (let [tool-kw (::d/selected-tool db),
          tool  (tool-kw tools/tools-map)]
      (tools/on-drag tool db dpos obj-id))))
