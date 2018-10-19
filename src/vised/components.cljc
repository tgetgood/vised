(ns vised.components
  (:require [clojure.string :as str]
            [falloleen.core :as f]
            [falloleen.math :as math]))

(f/deftemplate code-panel
  {:code "" :size 12}
  (let [lines (str/split-lines code)
        tw (math/floor (/ (math/log (count lines)) (math/log 10)))]
    (f/with-style {:font (str size "px monospace")}
      [(-> f/line
           (f/style {:opacity 0.4}))
       (map-indexed (fn [i line]
                      (-> [(f/translate (assoc f/text :text line)
                                           [(* (inc tw) size 1.3) 0])
                           (assoc f/text :text (str (inc i) ":"))]
                          (f/translate [0 (* -1 (* size 1.3) i)])))
                    lines)])))
