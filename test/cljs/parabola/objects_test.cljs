(ns parabola.objects-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.objects :as obj]
            [parabola.domain :as d]
            [clojure.spec.alpha :as s]))


(deftest path->svg-test
  (testing "path->svg (linear)"
    (let [[g_tag g_props [tag props & rest]]
          (obj/object->svg
            {::d/object-type :path
             ::d/id 0
             ::d/vertices
             [{::d/vertex-type :no-handles ::d/position [100 100]}
              {::d/vertex-type :no-handles ::d/position [200 200]}
              {::d/vertex-type :no-handles ::d/position [300 300]}]})]
      (is (= (:id g_props) 0))
      (is (= tag :path))
      (is (= (:d props) "M100,100 L200,200 L300,300")))))

  ;; Note that it doesn't really make sense to start a path with a symmetric,
  ;; as there is nothing on the LHS.
;  (testing "path->svg (symmetric corners)"
;    (let [[tag props] (obj/object->svg
;                         {::d/object-type :path
;                          ::d/id 0
;                          ::d/corners [[100 100 10 10] [200 200 20 20] [300 300 30 30]]))
;      (is (= (:id props) 0))
;      (is (= tag :path))
;      (is (= (:d props) "M100,100 S20,20 200,200 S30,30 300,300"))))
;
;  (testing "path->svg (asymmetric corners)"
;    (let [[tag props] (obj/object->svg
;                         {::d/object-type :path
;                          ::d/id 0
;                          ::d/corners [[100 100 0 0 11 11]
;                                       [200 200 20 20 22 22]
;                                       [300 300 30 30]))
;      (is (= (:id props) 0))
;      (is (= tag :path))
;      (is (= (:d props) "M100,100 C11,11 20,20 200,200 C22,22 30,30 300,300"))))
;
;  (testing "path->svg (curve then line)"
;    (let [[tag props] (obj/object->svg
;                         {::d/object-type :path
;                          ::d/id 0
;                          ::d/corners
;                          [
;                           [10  10   0  0 20 20]
;                           [50  10  40 20 50 10]
;                           [90  10  90 10]))
;      (is (= (:id props) 0))
;      (is (= tag :path))
;      (is (= (:d props) "M10,10 C20,20 40,20 50,10 C50,10 90,10 90,10")))))
;
;(deftest circle->svg-test
;  (testing "circle->svg"
;    (let [[tag props] (obj/object->svg
;                         {::d/object-type :circle
;                          ::d/id 999
;                          ::d/position [100 100]
;                          ::d/radius 50))
;      (is (= (:id props) 999))
;      (is (= tag :circle))
;      (is (= (:cx props) "100"))
;      (is (= (:cy props) "100"))
;      (is (= (:r props) "50")))))
;
