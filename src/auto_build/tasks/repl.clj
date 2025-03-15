(ns auto-build.tasks.repl
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [babashka.process       :as p]
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

(defn repl*
  [app-dir repl-aliases repl-port]
  (-> (p/shell {:continue true
                :dir app-dir}
               "clojure" (str "-X:" (str/join "" repl-aliases))
               ":port" repl-port)
      :exit))

(defn repl
  [{:keys [title]
    :as _printers}
   app-dir
   current-task
   repl-aliases
   repl-port]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg (str "Start clj repl - alias " repl-aliases)]
      (title title-msg)
      (repl* app-dir repl-aliases repl-port))))
