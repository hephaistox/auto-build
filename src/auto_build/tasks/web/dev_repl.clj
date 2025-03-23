(ns auto-build.tasks.web.dev-repl
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as    build-cmd
                           :refer [printing]]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn cljs-repl-watch*
  "Generate cljs repl"
  [{:keys [uri-str normalln errorln]} app-dir app-name]
  (normalln "Build " (uri-str app-name) " frontend in production mode")
  (printing ["npx" "shadow-cljs" "watch" app-name] app-dir normalln errorln 100))

(defn cljs-repl-watch
  "Build the project in production mode, and deploy remotely

  `uberjar-aliases` is a string of keywords that should be assembled to build the jar (called with -T)
  `app-name` if the name as found in `shadow-cljs.edn`"
  [{:keys [title _errorln]
    :as printers}
   app-dir
   current-task
   app-name]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Starts web dev repl"]
      (title title-msg)
      (-> (cljs-repl-watch* printers app-dir app-name)
          (build-cmd/status-to-exit-code printers title-msg)))))
