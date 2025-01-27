(ns auto-build.tasks.lint
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as    build-cmd
                           :refer [execute-if-success]]
   [clojure.string         :as str]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn lint-cmd
  "Lint command in `paths`. If `debug?` is set, that informations are displayed."
  [debug? paths]
  (when-not (empty? paths)
    (-> (concat ["clj-kondo" "--parallel"]
                (when debug? ["--debug"])
                ["--lint"]
                (mapv #(if (string? %) % (.getFile %)) paths)
                paths)
        vec)))

(defn lint*
  [{:keys [uri-str]
    :as printers}
   app-dir
   subdirs]
  (-> {:status :success}
      (execute-if-success printers
                          app-dir
                          verbose
                          (lint-cmd verbose subdirs)
                          (->> subdirs
                               (mapv uri-str)
                               (str/join ", ")
                               (str "Lint subdirs: "))
                          "Linting has failed"
                          nil)))

(defn lint
  "Lint `paths`

  if `debug?` is `true`,  linter provides detailed informations"
  [{:keys [title]
    :as printers}
   subdirs
   app-dir
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Linting"]
      (title title-msg)
      (-> (lint* printers app-dir subdirs)
          (build-cmd/status-to-exit-code printers title-msg)))))
