(ns parabola.utils
  (:require [clojure.spec.alpha :as s]
            [parabola.domain :as d]
            [adzerk.cljs-console :as log :include-macros true]))

(defn valid?
  "A version of clojure.spec.alpha/valid? that logs on failure"
  [spec obj]
  (if (s/valid? spec obj)
      true
      (let [result (s/explain-str spec obj)]
          (println result)
          false)))

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

(defn vector-length [vec] {:pre (valid? ::d/position vec)}
  (let [x (vec 0), y (vec 1)]
    (js.Math.sqrt (+ (* x x) (* y y)))))
