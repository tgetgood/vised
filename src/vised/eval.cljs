(ns vised.eval
  "Thin wrapper around Oakes' code at https://github.com/oakes/eval-soup"
  (:require [cljs.js :as cljs]
            [clojure.string :as str])
  (:import goog.net.XhrIo))

;; This is just copy paste
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

;; This too
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

(defn eval [form cb & [err]]
  (cljs/eval (cljs/empty-state) form #(if-let [v (:value %)]
                       (cb v)
                       (when err
                         (err %)))))
