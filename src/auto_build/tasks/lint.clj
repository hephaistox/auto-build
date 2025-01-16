(ns auto-build.tasks.lint
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.os.cmd :as build-cmd]
            [auto-build.os.exit-codes :as build-exit-codes]))

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

(defn lint
  "Lint `paths`

  if `debug?` is `true`,  linter provides detailed informations"
  [{:keys [normalln errorln title], :as _printers} subdirs app-dir current-task]
  (let [debug? verbose
        exit-code (build-cli-opts/enter cli-opts current-task)]
    (title "Linting")
    (if exit-code
      exit-code
      (let [linting-cmd-res (if verbose
                              (build-cmd/printing (lint-cmd debug? subdirs)
                                                  app-dir
                                                  normalln
                                                  errorln
                                                  10)
                              (build-cmd/print-on-error (lint-cmd debug?
                                                                  subdirs)
                                                        app-dir
                                                        normalln
                                                        errorln
                                                        10
                                                        100
                                                        100))]
        (if (= :success (:status linting-cmd-res))
          build-exit-codes/ok
          build-exit-codes/general-errors)))))
