(ns auto-build.tasks.formatting
  (:refer-clojure :exclude [format])
  (:require
   [auto-build.code.formatter :as build-formatter]
   [auto-build.os.cli-opts    :as build-cli-opts]
   [auto-build.os.cmd         :as    build-cmd
                              :refer [execute-if-success]]))

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

(defn format*
  "Format project"
  [printers app-dir]
  (let [formatter-setup (build-formatter/formatter-setup printers)]
    (when (= (:status formatter-setup) :success)
      (-> {:status :success}
          (execute-if-success
           printers
           app-dir
           verbose
           ["fd" "-e" "clj" "-e" "cljc" "-e" "cljs" "-e" "edn" "-x" "zprint" "-w"]
           "Format all files"
           "Error during formatting"
           nil)))))

(defn format
  "Format project"
  [{:keys [title]
    :as printers}
   app-dir
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Formatting"]
      (title title-msg)
      (-> (format* printers app-dir)
          (build-cmd/status-to-exit-code printers title-msg)))))
