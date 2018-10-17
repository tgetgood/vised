(ns vised.core
  (:require [chord.http-kit :refer [wrap-websocket-handler]]
            [org.httpkit.server :as http]
            [clojure.core.async :as async]
            [falloleen.core :as fc]))

(defonce connection (atom nil))

(defn handler [{:keys [ws-channel] :as req}]
  (async/go
    (reset! connection ws-channel)))

(defonce stop (atom nil))

(defn stop! []
  (@stop))

(defn init! []
  (reset! stop (http/run-server (-> #'handler wrap-websocket-handler) {:port 3333})))

(def code-str
  "(-> [(assoc fc/circle :radius 200)
         (assoc fc/line :to [500 1000])]
       (fc/translate [200 200]))")

(def form (read-string code-str))

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
