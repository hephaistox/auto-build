(ns auto-build.project.config
  "Loads `project` configuration file."
  (:refer-clojure :exclude [read])
  (:require [auto-build.os.edn-utils :as build-edn]
            [auto-build.os.filename :as build-filename]))

(def project-cfg-filename "project.edn")

(defn filename
  "Returns the `project.edn` filename of the project in `app-dir`."
  [app-dir]
  (build-filename/create-file-path app-dir project-cfg-filename))

(defn read
  "Returns the project configuration file descriptor in `app-dir`."
  [printers app-dir]
  (->> (filename app-dir)
       (build-edn/read-edn printers)))
