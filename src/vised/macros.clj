(ns vised.macros)

(def types
  '[AffineWrapper
    FixedTranslation
    Circle
    Line
    Bezier
    RawText
    Spline
    ClosedSpline])

(defn fqdn [sym]
  (symbol (str "falloleen.lang." (name sym))))

(defn map-construct [sym]
  (symbol "falloleen.lang" (str "map->" (name sym))))

(defmacro init-reader! []
  `(do
     (cljs.reader/register-tag-parser! ~'atom (fn [v#] (atom (:val v#))))
    ~@(map (fn [t]
             `(cljs.reader/register-tag-parser! '~(fqdn t) ~(map-construct t)))
           types)))
