(ns parabola.db-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.domain]
            [clojure.spec.alpha :as s]))

(defn mouse [n]
  "<:3)")

(deftest mouse-test
  (testing "it has a cute tail"
    (are [n m] (= (mouse n) m)
      0 "<:3)"
      1 "<:3~)")))

;; These are just testing the spec; so, a bit pointless really
;;

(deftest position-test
  (testing "position spec"
    (are [data v] (= (s/valid? :parabola.domain/position data) v)
      [100 100] true
      [100 100 100] false
      ["100" 100] false)))

(deftest is-db-valid
  (testing "that the DB is valid"
    (is (s/valid? :parabola.domain/db parabola.db/default-db**))))

;  (:require [clojure.test.check :as tc]
;            [clojure.test.check.generators :as gen]
;            [clojure.test.check.properties :as prop :include-macros true])

;(def sort-idempotent-prop
;  (prop/for-all [v (gen/vector gen/int)]
;    (= (* (sort v) 2) (sort (sort v))))

;(tc/quick-check 100 sort-idempotent-prop)
;; => {:result true, :num-tests 100, :seed 1382488326530}
