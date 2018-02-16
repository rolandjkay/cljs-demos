(ns parabola.db)

(def default-db
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

(defrecord Position [x y])
(defrecord Anchor [position handle-one handle-two])
(defrecord Path [id anchors])

; The state for the move tool.
; - This keeps track of the type of object that is currently selected -- e.g.
;   :path :circle; the ID of the object; the initial position of the object
;   and the client position of the click that started the move.
;
;   The meaning of the ID is to be interpreted by the selected object. For
;   example, for a path [2 3 :position] means the path with ID 2/forth
;   anchor/position of. [0 0 :handle-one] means path 0/first anchor/first handle
(defrecord MoveToolState [object-type object-id origin client-origin])
(def null-move-tool-state (MoveToolState. :none :none nil nil))

(def default-db*
  {
    :objects
    {
      :path [(Path. 0 [(Anchor. (Position. 25 50) (Position. 25 150) nil)
                       (Anchor. (Position. 75 100) (Position. 75 150) nil)
                       (Anchor. (Position. 150 75) (Position. 100 25) nil)])]}

    :selected-tool :move
    :move-tool-state null-move-tool-state})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-db**
  {
    :objects [
              [:path
                [ 0
                  [
                   [ [25 50] [25 150]]
                   [ [75 100] [75 150]]
                   [ [150 75] [100 25]]]]]]

    :selected-tool :move
    :move-tool-state ::none})
