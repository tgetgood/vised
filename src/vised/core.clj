(ns vised.core
  (:require [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [falloleen.core :as fc]
            [org.httpkit.server :as http]
            [vised.components :as components]))

(defonce connection (atom nil))

(defonce connections (atom {}))
(defonce view-map (atom {}))

(defonce stop (atom nil))

(defn stop! []
  (@stop))

(def code
  )

(def form
  '(-> [(assoc fc/circle :radius 200)
        (assoc fc/line :to [500 1000])]
       (fc/translate [200 200])))

(def dimensions [1056 1106])

(defn code-window [code]
  (let [[w h] dimensions]
    (fc/translate
     (assoc components/code-panel :code code)
     [10 (- h 30)])))

(defn frame [shape]
  (let [text-width 500
        [w h] dimensions
        box (assoc fc/rectangle :width (- w text-width) :height h)]
    (-> [(fc/clip shape box)
         (fc/style box {:stroke :grey :fill :none :opacity 0.1})]
        (fc/translate [text-width 0]))))

(defn screen [code shape]
  [(code-window code)
   (frame shape)])

(defn send-to! [x]
  (require '[falloleen.core :as fc])
  (let [image (screen (with-out-str (pprint form))
                      (eval form))]
    (async/put! x image)))

(defn send-code! []
  (when @connection
    (send-to! @connection)))

;;;;; Server

(defn handler [{:keys [ws-channel] :as req}]
  (async/go
    (async/>! ws-channel :identify)
    (let [id (:message (async/<! ws-channel))]
      (swap! connections assoc id ws-channel)
      (when-let [view (get @view-map id)]
        (async/>! ws-channel view)))))

(defn init! []
  (reset! stop (http/run-server (-> #'handler wrap-websocket-handler) {:port 3333})))
