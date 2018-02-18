;; Tests to check our db schema
;;
(ns parabola.domain-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.domain :as d]
            [clojure.spec.alpha :as s]))

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
          {::d/object-type :path
           ::d/id          0
           ::d/corners [
                        [25 50, 25 150]        ; Symmetric corner
                        [75 100, 75 150]       ; Symmetric corner
                        [10 10]                ; Sharp corners
                        [34 50, 12 40, 60 50]  ; Asymmetric corner
                        [150 75, 100 25]]}))))

(deftest circle-test
  (testing "circle spec"
    (is (s/valid? ::d/circle
          {::d/object-type :circle
           ::d/id          2
           ::d/position    [100 100]
           ::d/radius      100}))))
