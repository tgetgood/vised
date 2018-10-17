(ns vised.components
  (:require [clojure.string :as str]
            [falloleen.core :as f]))

(f/deftemplate code-panel
  {:code "" :size 15}
  (let [lines (str/split-lines code)
        tw (js/Math.floor (/ (js/Math.log (count lines)) (js/Math.log 10)))]
    (f/with-style {:font (str size "px monospace")}
      [(-> f/line
           (assoc :from [tw 0] :to [tw 2000]) ; HACK: grab the screen height
           (f/style {:opacity 0.4}))
       (map-indexed (fn [i line]
                      (-> [(f/translate (assoc f/text :text line)
                                           [(* (inc tw) size 1.3) 0])
                           (assoc f/text :text (str (inc i) ":"))]
                          (f/translate [0 (* -1 (* size 1.3) i)])))
                    lines)])))
