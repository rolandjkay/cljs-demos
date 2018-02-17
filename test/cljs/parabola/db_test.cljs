(ns parabola.db-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.domain :as d]
            [parabola.db]
            [clojure.spec.alpha :as s]))

;; These are just testing the spec.
;;

(deftest position-test
  (testing "position spec"
    (are [data v] (= (s/valid? ::d/position data) v)
      [100 100] true
      [100 100 100] false
      ["100" 100] false)))

(deftest corner-test
  (testing "corner spec"
    (is (s/valid? ::d/corner [100 100]))
    (is (s/valid? ::d/corner [100 100 200 200]))
    (is (s/valid? ::d/corner [100 100 200 200 300 300]))))


(deftest path-test
  (testing "path spec"
    (is (s/valid? ::d/path
          [:path 0
                 [
                  [25 50, 25 150]        ; Symmetric corner
                  [75 100, 75 150]       ; Symmetric corner
                  [10 10]                ; Sharp corners
                  [34 50, 12 40, 60 50]  ; Asymmetric corner
                  [150 75, 100 25]]]))))

(deftest is-db-valid
  (testing "that the DB is valid"
    (is (s/valid? ::d/db parabola.db/default-db**))))

;  (:require [clojure.test.check :as tc]
;            [clojure.test.check.generators :as gen]
;            [clojure.test.check.properties :as prop :include-macros true])

;(def sort-idempotent-prop
;  (prop/for-all [v (gen/vector gen/int)]
;    (= (* (sort v) 2) (sort (sort v))))

;(tc/quick-check 100 sort-idempotent-prop)
;; => {:result true, :num-tests 100, :seed 1382488326530}
