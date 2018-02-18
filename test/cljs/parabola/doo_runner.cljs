(ns parabola.doo-runner
    (:require [doo.runner :refer-macros [doo-tests doo-all-tests]]
              [parabola.db-test]
              [parabola.domain-test]
              [parabola.objects-test]))

(enable-console-print!)
(doo-tests 'parabola.db-test
           'parabola.domain-test
           'parabola.objects-test)
