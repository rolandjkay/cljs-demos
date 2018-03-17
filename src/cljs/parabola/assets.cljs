;;
;; Assets
;;
(ns parabola.assets)


(defn node-handle-after []
  [:svg {:width 32 :height 32}
   ;[:rect {:x 0 :y 0 :width 32 :height 32 :fill "none" :stroke "black"}]
   [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
     [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
     [:path {:fill "none"
             :stroke-width "6"
             :stroke-linecap "round"
             :stroke-miterlimit "10"
             :d "M52,52 m-24,35 l29,-35 c33.5,0.8 40,-25.2 40,-25.2"}]]])

(defn node-handle-before []
  [:svg {:width 32 :height 32}
   ;[:rect {:x 0 :y 0 :width 32 :height 32 :fill "none" :stroke "black"}]
   [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
     [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
     [:path {:fill "none"
             :stroke-width "6"
             :stroke-linecap "round"
             :stroke-miterlimit "10"
             :d "M62,52 m24,35 l-29,-35 c-33.5,0.8 -40,-25.2 -40,-25.2"}]]])


(defn node-semi-symmetric []
  [:svg {:width 32 :height 32}
   [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
     [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
     [:path {:fill "none"
             :stroke-width "6"
             :stroke-linecap "round"
             :stroke-miterlimit "10"
             :d "M52,52 m-35,30 c0,0 19.5-77.5 41.5-28 c7.2,16.3 30,28.8 30,28.8"}]]])

(defn node-symmetric []
  [:svg {:width 32 :height 32}
    ;[:rect {:x 0 :y 0 :width 32 :height 32 :fill "none" :stroke "black"}]
    [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
      [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
      [:path {:fill "none"
              :stroke-width "6"
              :stroke-linecap "round"
              :stroke-miterlimit "10"
              :d "M52,52 m-35,27 c0,0,10.2-28,41.5-28c27.1,0,36.4,28.8,36.4,28.8"}]]])

(defn node-no-handles []
  [:svg {:width 32 :height 32}
    [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
      [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
      [:polyline {:fill "none"
                  :stroke-width "6"
                  :stroke-linecap "round"
                  :stroke-miterlimit "10"
                  :points "90,96 56,54 25,98"}]]])

(defn node-asymmetric []
  [:svg {:width 32 :height 32}
    ;[:rect {:x 0 :y 0 :width 32 :height 32 :fill "none" :stroke "black"}]
    [:g {:transform "scale(0.3)" :stroke "white" :fill "white"}
      [:path {:d "M52,52 m-5,0 l10,-10 l10,10 l-10,10 z"}]
      [:path {:fill "none"
              :stroke-width "6"
              :stroke-linecap "round"
              :stroke-miterlimit "10"
              :d "M52,52 m-43,0 c0,0,24.5-33.5,49,0c22.2-33.5,49,0,49,0"}]]])
