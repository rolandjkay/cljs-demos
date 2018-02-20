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
      (is (= (:d props) "M100.0,100.0 L200.0,200.0 L300.0,300.0"))))

  ;; Note that it doesn't really make sense to start a path with a symmetric,
  ;; as there is nothing on the LHS.
  (testing "path->svg (symmetric corners)"
    (let [[g_tag g_props [tag props & rest]]
          (obj/object->svg
            {::d/object-type :path
             ::d/id 0
             ::d/vertices
             [{::d/vertex-type :handle-after ::d/position [100 100] ::d/after-angle 0 ::d/after-length 0}
              {::d/vertex-type :symmetric ::d/position [200 200] ::d/angle 0 ::d/length 10}
              {::d/vertex-type :handle-before ::d/position [300 300] ::d/before-angle 90 ::d/before-length 10}]})]
             ; [[100 100 10 10] [200 200 20 20] [300 300 30 30]]})]
      (is (= (:id g_props) 0))
      (is (= tag :path))
      (is (= (:d props) "M100.0,100.0 C100.0,100.0 190.0,200.0 200.0,200.0 C210.0,200.0 300.0,290.0 300.0,300.0"))))

  (testing "path->svg (asymmetric corners)"
    (let [[g_tag g_props [tag props & rest]]
          (obj/object->svg
            {::d/object-type :path
             ::d/id 0
             ::d/vertices
             [{::d/vertex-type :handle-after ::d/position [10 10] ::d/after-angle 45 ::d/after-length 14}
              {::d/vertex-type :handle-before ::d/position [50 10] ::d/before-angle -45 ::d/before-length 14}
              {::d/vertex-type :no-handles ::d/position [90 10]}]})]

      (is (= (:id g_props) 0))
      (is (= tag :path))
      (is (= (:d props) "M10.0,10.0 C19.9,19.9 40.1,19.9 50.0,10.0 L90.0,10.0")))))


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
