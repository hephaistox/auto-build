(ns auto-build.project.shadow
  (:refer-clojure :exclude [read])
  (:require [auto-build.os.edn-utils :as build-edn-utils]
            [auto-build.os.filename :as build-filename]))

(defn shadow-cljs-edn-filename
  [app-dir]
  (build-filename/create-file-path app-dir "shadow-cljs.edn"))

(defn read
  "Returns a map with
  * `:filepath`
  * `:afilepath`
  * `:raw-content` if file can be read.
  * `:invalid?` is boolean
  * `:exception` if something wrong happened.
  * `:edn` if the translation is succesful."
  [printers app-dir]
  (->> app-dir
       shadow-cljs-edn-filename
       (build-edn-utils/read-edn printers)))

(defn write-edn
  "Returns
  * `:filepath` as given as a parameter
  * `:afilepath` file with absolute path
  * `:status` is `:success` or `:fail`
  * `:raw-content`
  * `:exception` (only if `:status` is `:fail`)"
  [printers app-dir content]
  (build-edn-utils/write-edn printers
                             (shadow-cljs-edn-filename app-dir)
                             content))
