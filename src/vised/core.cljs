(ns ^:figwheel-hooks vised.core
  (:require [chord.client :as chord]
            [cljs.core.async :as async :include-macros true]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [falloleen.core :as fall]
            [falloleen.hosts :as hosts])
  (:require-macros [vised.macros :as macros]))

(enable-console-print!)

(macros/init-reader-for!
 falloleen.lang
 falloleen.core)

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

(defonce host (hosts/default-host {:size :fullscreen}))

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
              (reset! v message)
              (let [image (:message message)]
                (fall/draw! image host)
                (recur)))))))))

(defn ^:export init []
  (init-ws-connection))

(defn ^:after-load on-js-reload []
  (init))
