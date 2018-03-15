;;
;; Add syntax highlighting to Hiccup-style XML
;;
(ns parabola.highlight
  (:require [clojure.spec.alpha :as s]))

(defn- repeat-str [n s]
  (apply str (repeat n s)))

(defn- handle-attrib [i [k v]]
  (list
    [:span.hl-attrib {:key (str i "-key")}
      (name k)]
    "="
    [:span.hl-string {:key (str i "-value")}
      (str "\"" v "\"")]
    " "))

(defn- handle-element [depth i [tag attribs & rest]]
  (let [children rest ;(if (map? attribs) rest (conj rest attribs))]
        handle-element* (partial handle-element (inc depth))
        indent (repeat-str (* 2 depth) " ")]
    [:span.hl-tag {:key (str i "-start")}
      indent
      "<"
      [:span.hl-name tag (if (> (count attribs) 0) " ")]
      (map-indexed handle-attrib attribs)
      (if children ">\n" "/>\n")
      (map-indexed handle-element* children)
      (if children
        (list indent "</" [:span.hl-name {:key (str i "-end")} tag ">\n"]))]))


(defn highlight
  "
  [:circle {:position \"10,10\" radius \"20\"}]

  is converted to

  <pre><code>
    [:span.hljs-tag \"<\"
      [:span.hljs-name \"circle\"]
      [:span.hljs-attrib \"position\"]
      \"=\"
      [:span.hljs-string \"10,10\"]
      [:span.hljs-attrib \"radius\"]
      \"=\"
      [:span.hljs-string \"20\"] \"/>\"
  </code></pre>
  "
  [in]
  (println "in-->" in)
  [:pre
    [:code.hl.xml
      (handle-element 0 0 in)]])
