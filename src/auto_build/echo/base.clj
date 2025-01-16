(ns auto-build.echo.base
  "Common functions for echoing."
  (:require [auto-build.os.colorized-text :as build-text]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; ********************************************************************************
;; Store echoing parameters
;; Each bb tasks is executed in one environment, with one width
;; ********************************************************************************

(def echo-param "Global echo configuration." (atom {:width 240}))
(def normal-font-color build-text/font-default)
(def error-font-color build-text/font-red)

; ********************************************************************************
; Public API
; ********************************************************************************
(defn uri-str "Returns the string of the `uri`." [uri] (str "`" uri "`"))
(defn cmd-str
  "Returns the string of the `cmd`."
  [cmd]
  (let [cmd (if (string? cmd) cmd (.getFile cmd))] (str "`" cmd "`")))

(defn exception-str [e] (pr-str e))
(defn exceptionln
  [e]
  (-> e
      exception-str
      println))

(defn current-time-str
  "Returns current time string."
  []
  (.format (java.text.SimpleDateFormat. "HH:mm:ss:SSS") (java.util.Date.)))

(defn build-writter
  "Creates a writter that could be binded to the output with `(binding [*out* s])` where `s` is the result of this function."
  []
  (new java.io.StringWriter))

(defn print-writter
  "Print `str-writter` if necessary it contains something."
  [str-writter]
  (let [str-writter (str str-writter)]
    (when-not (str/blank? str-writter) (print str-writter))))

(defn pprint-str "Pretty print `data`" [data] (with-out-str (pp/pprint data)))

(def normalln println)

(defn errorln
  [& texts]
  (print build-text/bg-red)
  (apply println texts)
  (print build-text/style-reset-all))

(comment
  (normalln "argz")
  (errorln "arg")
  ;
)

(def printers
  "Generic printers defined - printing raw text on the terminal"
  {:cmd-str cmd-str,
   :uri-str uri-str,
   :exception-str exception-str,
   :exceptionln exceptionln,
   :current-time-str current-time-str,
   :build-writter build-writter,
   :print-writter print-writter,
   :pprint-str pprint-str,
   :normalln normalln,
   :errorln errorln})
