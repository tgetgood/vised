(ns vised.macros
  (:require [clojure.string :as string]))

(defn lang? [c]
  (string/starts-with? (.getName c) "falloleen."))

(defn ns-defined-types [ns-sym]
  (let [all (into [] (comp (map val)
                           (remove var?)
                           (filter lang?))
                  (ns-map (the-ns ns-sym)))]
    all))

(defn map-construct [c]
  (let [bits (string/split (.getName c)
                           (re-pattern (str "." (.getSimpleName c) "$")))
        ns (apply str bits)]
    (symbol ns (str "map->" (.getSimpleName c)))))

(defmacro init-reader-for! [& nses]
  `(do
     (cljs.reader/register-tag-parser! ~'atom (fn [v#] (atom (:val v#))))
     ~@(map
        (fn [ns]
          `(do
             ~@(map (fn [t]
                      `(cljs.reader/register-tag-parser!
                        '~(symbol (.getName t)) ~(map-construct t)))
                    (ns-defined-types ns))))
        nses)))
