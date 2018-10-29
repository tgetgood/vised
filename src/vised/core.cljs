(ns ^:figwheel-hooks vised.core
  (:require [chord.client :as chord]
            [cljs.core.async :as async :include-macros true]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [falloleen.core :as fall]
            [falloleen.hosts :as hosts]
            [vised.components :as components])
  (:require-macros [vised.reflection :refer [init-reader-for!]]))

(defn get-or-create-uuid! []
  (if-let [id (js/sessionStorage.getItem "window-id")]
    (read-string id)
    (let [uuid (cljs.core/random-uuid)]
      (js/sessionStorage.setItem "window-id" (pr-str uuid))
      uuid)))

(enable-console-print!)

(init-reader-for!
 vised.components
 falloleen.lang
 falloleen.core)

(defonce host (hosts/default-host {:size :fullscreen}))

(defonce ws-ch (atom nil))
(defonce v (atom nil))

(defn init-ws-connection [uuid]
  (when @ws-ch
    (async/close! @ws-ch))
  (async/go
    (let [{:keys [ws-channel error]} (<! (chord/ws-ch "ws://localhost:3333"))]
      (if error
        (.error js/console error)
        (do
          (async/<! ws-channel)
          (async/>! ws-channel uuid)
          (reset! ws-ch ws-channel)
          (async/go-loop []
            (when-let [message (async/<! @ws-ch)]
              (reset! v message)
              (let [image (:message message)]
                (fall/draw! image host)
                (recur)))))))))

(defn ^:export init []
  (let [id (get-or-create-uuid!)]
    (init-ws-connection id)))

(defn ^:after-load on-js-reload []
  (.log js/console "Reload!")
  (init))
