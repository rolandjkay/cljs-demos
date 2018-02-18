(ns parabola.db-test
  (:require [clojure.test :refer [deftest testing is are run-tests]]
            [parabola.domain :as d]
            [parabola.db]
            [clojure.spec.alpha :as s]))



(deftest is-db-valid
  (testing "that the DB is valid"
    (is (s/valid? ::d/db parabola.db/default-db))))

;  (:require [clojure.test.check :as tc]
;            [clojure.test.check.generators :as gen]
;            [clojure.test.check.properties :as prop :include-macros true])

;(def sort-idempotent-prop
;  (prop/for-all [v (gen/vector gen/int)]
;    (= (* (sort v) 2) (sort (sort v))))

;(tc/quick-check 100 sort-idempotent-prop)
;; => {:result true, :num-tests 100, :seed 1382488326530}
