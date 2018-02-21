(ns parabola.utils)

(defn pairs
  "Generate a sequence of all pairs in xs"
  [xs]
  (let [xs* (seq xs)]
    (map vector (butlast xs*) (next xs*))))

(defn value-in-collection?
  [x coll]
  (boolean (some #(= x %) coll)))
