(ns parabola.objects-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.objects :as obj]
            [parabola.domain :as d]
            [clojure.spec.alpha :as s]))

(deftest path->svg-test
  (testing "path->svg"
    (let [[tag props] (obj/object->svg
                         {::d/object-type :path
                          ::d/id 0
                          ::d/corners [[100 100] [200 200] [300 300]]})]
      (is (= (:id props) 0))
      (is (= tag :path))
      (is (= (:d props) "M100,100 L200,200 L300,300")))))

(deftest circle->svg-test
  (testing "circle->svg"
    (let [[tag props] (obj/object->svg
                         {::d/object-type :circle
                          ::d/id 999
                          ::d/position [100 100]
                          ::d/radius 50})]
      (is (= (:id props) 999))
      (is (= tag :circle))
      (is (= (:cx props) "100"))
      (is (= (:cy props) "100"))
      (is (= (:r props) "50")))))
