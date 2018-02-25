(ns parabola.log)

(defn info [& args]
  (apply js/console.info args))

(defn warn [& args]
  (apply js/console.warn args))

(defn error [& args]
  (apply js/console.error args))

(defn log-assert [condition & args]
  (if (not condition)
      (apply error args)))
