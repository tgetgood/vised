(ns ^:figwheel-hooks vised.core
  (:require-macros [vised.macros :as macros])
  (:require [chord.client :as chord]
            [cljs.core.async :as async :include-macros true]
            [cljs.tools.reader.edn :as edn]
            [cljs.js :as cljs]
            [cljs.pprint :refer [pprint]]
            [cljs.reader :refer [read-string] :refer-macros [add-data-readers]]
            [clojure.string :as str]
            [falloleen.core :as fall]
            [falloleen.hosts :as hosts])
  (:import goog.net.XhrIo))

(enable-console-print!)
(macros/init-reader!)

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
  {:code "" :size 15}
  (let [lines (str/split-lines code)
        tw (js/Math.floor (/ (js/Math.log (count lines)) (js/Math.log 10)))]
    (fall/with-style {:font (str size "px monospace")}
      [
       (map-indexed (fn [i line]
                      (-> [(fall/translate (assoc fall/text :text line)
                                           [(* (inc tw) size 1.3) 0])
                           (assoc fall/text :text (str (inc i) ":"))]
                          (fall/translate [0 (* -1 (* size 1.3) i)])))
                    lines)])))

(def code-str
  "(-> [(assoc falloleen.core/circle :radius 200)
         (assoc falloleen.core/line :to [500 1000])]
       (falloleen.core/translate [200 200]))")

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
     [10 (- h 30)])))

(defn frame [shape]
  (let [text-width 500
        [w h] (fall/dimensions host)
        box (assoc fall/rectangle :width (- w text-width) :height h)]
    (-> [(fall/clip shape box)
         (fall/style box {:stroke :grey :fill :none :opacity 0.1})]
        (fall/translate [text-width 0]))))

(defn screen [code shape]
  [(code-window code)
   (frame shape)])


(defonce ws-ch (atom nil))
(defonce v (atom nil))

(defn init-ws-connection []
  (async/go
    (let [{:keys [ws-channel error]} (<! (chord/ws-ch "ws://localhost:3333"))]
      (if error
        (.error js/console error)
        (do
          (reset! ws-ch ws-channel)
          (async/go-loop []
            (when-let [message (async/<! @ws-ch)]
              (println message)
              (reset! v (:message message))
              (let [image (:message message)]
                (fall/draw! image host)
                (recur)))))))))

(defn ^:export init []
  (init-ws-connection)

  #_(cljs/eval s code-form #(fall/draw! (screen pretty-code-str (:value %)) host)))

(defn tc []
  (when @ws-ch
    (async/go
      (.log js/console @ws-ch)
      (async/>! @ws-ch "is it safe?")
      (.log js/console (async/<! @ws-ch)))))

(defn ^:after-load on-js-reload []
  (init)
  )


(cljs.reader/register-tag-parser! 'atom (fn [x] (atom nil)))
(println (read-string @v))
