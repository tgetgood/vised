(ns ^:figwheel-hooks vised.core
  (:import goog.net.XhrIo)
  (:require [cljs.js :as cljs]
            [cljs.pprint :refer [pprint]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [falloleen.core :as fall]
            [falloleen.hosts :as hosts]))

(enable-console-print!)

(defn ^:private fix-goog-path [path]
  ; goog/string -> goog/string/string
  ; goog/string/StringBuffer -> goog/string/stringbuffer
  (let [parts (str/split path #"/")
        last-part (last parts)
        new-parts (concat
                    (butlast parts)
                    (if (= last-part (str/lower-case last-part))
                      [last-part last-part]
                      [(str/lower-case last-part)]))]
    (str/join "/" new-parts)))

(defn ^:private custom-load!
  ([opts cb]
   (if (re-matches #"^goog/.*" (:path opts))
     (custom-load!
      (update opts :path fix-goog-path)
      [".js"]
      cb)
     (custom-load!
      opts
      (if (:macros opts)
        [".clj" ".cljc"]
        [".cljs" ".cljc" ".js"])
      cb)))
  ([opts extensions cb]
   (if-let [extension (first extensions)]
     (try
       (.send XhrIo
              (str (:path opts) extension)
              (fn [e]
                (if (.isSuccess (.-target e))
                  (cb {:lang (if (= extension ".js") :js :clj)
                       :source (.. e -target getResponseText)})
                  (custom-load! opts (rest extensions) cb))))
       (catch js/Error _
         (custom-load! opts (rest extensions) cb)))
     (cb {:lang :clj :source ""}))))


(set! cljs/*eval-fn* cljs/js-eval)
(set! cljs/*load-fn* custom-load!)

(fall/deftemplate code-panel
  {:code ""}
  (let [lines (str/split-lines code)]
    (fall/with-style {:font "15px monospace"}
      (map-indexed (fn [i line]
                     (-> (assoc fall/text :text line)
                         (fall/translate [0 (* -1 16 i)])))
                   lines))))

(def code-str
  "(-> [(assoc falloleen.core/circle :radius 200)
         (assoc falloleen.core/line :to [500 1000])]
       (falloleen.core/translate [200 200])
  )")

(def code-form
  (read-string code-str))

(def pretty-code-str
  (with-out-str (pprint code-form)))

(defonce host (hosts/default-host {:size :fullscreen}))

(def s (cljs/empty-state))

(defn code-window [code]
  (let [[w h] (fall/dimensions host)]
    (fall/translate
     (assoc code-panel :code code)
     [10 (- h 100)])))

(defn frame [shape]
  (let [text-width 450
        [w h] (fall/dimensions host)
        box (assoc fall/rectangle :width (- w text-width) :height h)]
    (-> [(fall/clip shape box)
         (fall/style box {:stroke :grey :fill :none :opacity 0.1})]
        (fall/translate [text-width 0]))))

(defn screen [code shape]
  [(code-window code)
   (frame shape)])

(defn ^:export init []
  (cljs/eval s code-form #(fall/draw! (screen pretty-code-str (:value %)) host)))

(defn ^:after-load on-js-reload []
  (init)
)
