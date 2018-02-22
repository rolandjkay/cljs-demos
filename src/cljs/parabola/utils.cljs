(ns parabola.utils)

(defn pairs
  "Generate a sequence of all pairs in xs"
  [xs]
  (let [xs* (seq xs)]
    (map vector (butlast xs*) (next xs*))))

(defn value-in-collection?
  [x coll]
  (boolean (some #(= x %) coll)))

(defn- split-id
  "Split ID like 1/2/3 to [1, 2, 3]"
  [id]
  (mapv int (clojure.string/split id #"/")))

;; Add and subtract positions
(def pos-diff (partial mapv -))
(def pos-add (partial mapv +))
