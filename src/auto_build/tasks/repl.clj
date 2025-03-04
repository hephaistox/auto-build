(ns auto-build.tasks.repl
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [babashka.process       :as p]))

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

(defn repl*
  [app-dir]
  (-> (p/shell {:continue true
                :dir app-dir}
               "clojure"
               "-X:repl")
      :exit))

(defn repl
  [{:keys [title]
    :as _printers}
   app-dir
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Start clj repl"]
      (title title-msg)
      (repl* app-dir))))
