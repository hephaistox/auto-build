(ns auto-build.tasks.web.css
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

(defn css-repl-watch*
  "Generate cljs repl"
  [{:keys [normalln errorln]} app-dir css-file target-dir]
  (normalln "Build frontend in production mode")
  (let [cmd ["npx" "@tailwindcss/cli" "-i" css-file "-o" target-dir "--watch"]]
    (when verbose
      (-> cmd
          build-cmd/to-str
          print))
    (printing cmd app-dir normalln errorln 100)))

(defn css-repl-watch
  "Build the css file in dev mode, and deploy remotely

  `uberjar-aliases` is a string of keywords that should be assembled to build the jar (called with -T)
  `app-name` if the name as found in `shadow-cljs.edn`"
  [{:keys [title _errorln]
    :as printers}
   app-dir
   current-task
   css-file
   target-dir]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Starts web dev repl"]
      (title title-msg)
      (-> (css-repl-watch* printers app-dir css-file target-dir)
          (build-cmd/status-to-exit-code printers title-msg)))))
