(ns vised.core
  (:require [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [falloleen.core :as fc]
            [org.httpkit.server :as http]
            [vised.components :as components]))

(defonce connection (atom nil))

(defn handler [{:keys [ws-channel] :as req}]
  (async/go
    (reset! connection ws-channel)))

(defonce stop (atom nil))

(defn stop! []
  (@stop))

(defn init! []
  (reset! stop (http/run-server (-> #'handler wrap-websocket-handler) {:port 3333})))

(def code
  "(-> [(assoc fc/circle :radius 200)
         (assoc fc/line :to [500 1000])]
       (fc/translate [200 200]))")

(def form (read-string code))

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

(defn send-code! []
  (when @connection
    (async/put! @connection (screen (with-out-str (pprint form))
                                    (eval form)))))
