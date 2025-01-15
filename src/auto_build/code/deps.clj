(ns auto-build.code.deps
  (:refer-clojure :exclude [read])
  (:require [auto-build.os.edn-utils :as build-edn-utils]
            [auto-build.os.filename :as build-filename]))

(defn deps-edn-filename
  [app-dir]
  (build-filename/create-file-path app-dir "deps.edn"))

(defn read
  "Returns a map with
  * `:filepath`
  * `:afilepath`
  * `:raw-content` if file can be read.
  * `:invalid?` is boolean
  * `:exception` if something wrong happened.
  * `:edn` if the translation is succesfull."
  [app-dir]
  (-> app-dir
      deps-edn-filename
      build-edn-utils/read-edn))

(defn write-edn
  "Returns
  * `:filepath` as given as a parameter
  * `:afilepath` file with absolute path
  * `:status` is `:success` or `:fail`
  * `:raw-content`
  * `:exception` (only if `:status` is `:fail`)"
  [printer app-dir content]
  (-> app-dir
      deps-edn-filename
      (build-edn-utils/write-edn printer content)))
