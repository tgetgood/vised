(ns ^:figwheel-hooks vised.core
    (:require ))

(enable-console-print!)

(defn ^:export init []
  )

(defn ^:after-load on-js-reload []
  (init)
)
