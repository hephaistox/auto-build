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
    (-> (concat ["clj-kondo"]
                ;; Project still too small and parallel is not useful:
                ;; "--parallel"
                (when debug? ["--debug"])
                ["--lint"]
                paths)
        vec)))

(defn lint
  "Lint `paths`

  if `debug?` is `true`,  linter provides detailed informations"
  [{:keys [normalln errorln], :as _printers} subdirs app-dir]
  (let [debug? verbose]
    (normalln "Linting")
    (if (= :success
           (-> (lint-cmd debug? subdirs)
               (build-cmd/print-on-error app-dir normalln errorln 10 100 100)
               :status))
      build-exit-codes/ok
      build-exit-codes/general-errors)))
