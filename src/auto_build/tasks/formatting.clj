(ns auto-build.tasks.formatting
  (:refer-clojure :exclude [format])
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.code.formatter :as build-formatter]
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

(defn format
  "Format project"
  [{:keys [title], :as printers} app-dir]
  (title "Format")
  (if (= :success
         (-> (build-formatter/format-clj printers app-dir verbose)
             :status))
    build-exit-codes/ok
    build-exit-codes/general-errors))
