(ns vised.reflection
  (:require [clojure.string :as string]))

(defn contained-in? [ns]
  (fn [^java.lang.Class c]
    (string/starts-with? (.getName c) ns)))

(defn ns-defined-types [ns-sym]
  (let [all (into [] (comp (map val)
                           (remove var?)
                           (filter (contained-in? (name ns-sym))))
                  (ns-map (the-ns ns-sym)))]
    all))

(defn map-construct [^java.lang.Class c]
  (let [bits (string/split (.getName c)
                           (re-pattern (str "." (.getSimpleName c) "$")))
        ns (apply str bits)]
    (symbol ns (str "map->" (.getSimpleName c)))))

(defmacro init-reader-for! [& nses]
  `(do
     ;; HACK: This shouldn't be necessary. I don't actually see a problem with
     ;; sending atoms over the wire in principle if they contain plain edn, but
     ;; there's probably a reason...
     (cljs.reader/register-tag-parser! ~(quote 'atom) (fn [v#] (atom (:val v#))))
     ~@(map
        (fn [ns]
          `(do
             ~@(map (fn [^java.lang.Class c]
                      `(cljs.reader/register-tag-parser!
                        '~(symbol (.getName c)) ~(map-construct c)))
                    (ns-defined-types ns))))
        nses)))
