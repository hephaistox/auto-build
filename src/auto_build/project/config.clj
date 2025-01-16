(ns auto-build.project.config
  "Loads `project` configuration file."
  (:refer-clojure :exclude [read])
  (:require [auto-build.os.edn-utils :as build-edn]
            [auto-build.os.filename :as build-filename]))

(def project-cfg-filename "project.edn")

(defn filename
  "Returns the `project.edn` filename of the project in `app-dir`."
  [{:keys [errorln], :as printers} app-dir]
  (let [project-config-filedesc
          (build-filename/create-file-path app-dir project-cfg-filename)]
    (when-not (= :success (get project-config-filedesc :status))
      (errorln printers "project.edn is not found"))))

(defn read
  "Returns the project configuration file descriptor in `app-dir`."
  [printers app-dir]
  (->> app-dir
       (filename printers)
       (build-edn/read-edn printers)))
