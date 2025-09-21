(ns auto-build.project.bb
  "Utilities for `bb.edn` files."
  (:refer-clojure :exclude [read])
  (:require
   [auto-build.os.edn-utils :as build-edn-utils]
   [auto-build.os.filename  :as build-filename]))

(defn bb-edn-filename
  "A `bb.edn` fullname based on the `app-dir` - the directory containing the application"
  [app-dir]
  (build-filename/create-file-path app-dir "bb.edn"))

(defn read
  "Returns a map with:
  * `:filepath`
  * `:afilepath` absolute path to the file
  * `:raw-content` if file can be read
  * `:invalid?` is boolean
  * `:exception` if something wrong happened
  * `:edn` if the translation is successful"
  [printers app-dir]
  (->> app-dir
       bb-edn-filename
       (build-edn-utils/read-edn printers)))

(comment
  (bb-edn-filename ".")
  ;
  (read {:errorln println
         :uri-str #(format "`%s`" %)
         :exception-msg #(println "Error: " %)}
        ".")
  ;
)
