(ns auto-build.tasks.web.prod-build
  "Move the web app to production"
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as    build-cmd
                           :refer [execute-if-success execute-whateverstatus]]))

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

(defn build*
  "Generate jar"
  [{:keys [uri-str]
    :as printers}
   app-dir
   uberjar-aliases
   fe-app-name
   repo-url
   target-dir]
  (cond-> {:status :success}
    fe-app-name (execute-whateverstatus
                 printers
                 app-dir
                 verbose
                 ["npx" "shadow-cljs" "release" fe-app-name]
                 (str "Build " (uri-str fe-app-name) " frontend in production mode")
                 (str "Frontend building of " (uri-str fe-app-name) " has failed")
                 :git-status)
    true (execute-if-success printers
                             app-dir
                             verbose
                             ["rm" "-fr" target-dir]
                             "Clean previous build"
                             "Error during target directory cleaning"
                             :clean-dir)
    true (execute-if-success
          printers
          app-dir
          verbose
          ["clojure" (str "-T" uberjar-aliases) :production-dir (str "'\"" target-dir "\"'")]
          "Build uberjar"
          "Error during uberjar creation"
          :create-uberjar)
    true (execute-if-success printers
                             target-dir
                             verbose
                             ["git" "init" "-b" "master"]
                             "Creates a local repo"
                             "Error during local repo creation"
                             :init-repo)
    true (execute-if-success printers
                             target-dir
                             verbose
                             ["git" "remote" "add" "clever" repo-url]
                             "Link to remote repo"
                             "Error during remote repo linking"
                             :remote-repo)
    true (execute-if-success printers
                             target-dir
                             verbose
                             ["git" "add" "."]
                             "Add all to index"
                             "Error during index creation"
                             :create-stage)
    true (execute-if-success printers
                             target-dir
                             verbose
                             ["git" "commit" "-m" "\"auto\""]
                             "Link to remote repo"
                             "Error during remote repo linking"
                             :commit)))

(defn build
  "Build the project in production mode, and deploy remotely

  `uberjar-aliases` is a string of keywords that should be assembled to build the jar (called with -T)
  `fe-app-name` if the name as found in `shadow-cljs.edn`"
  [{:keys [title errorln]
    :as printers}
   app-dir
   current-task
   uberjar-aliases
   fe-app-name
   env-varname]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (if-let [repo-url (System/getenv env-varname)]
      (let [title-msg "Generate uberjar"]
        (title title-msg)
        (-> (build* printers app-dir uberjar-aliases fe-app-name repo-url)
            (build-cmd/status-to-exit-code printers title-msg)))
      (errorln "For security reasons, the repo url should be saved as a system environment"))))

(defn build-and-push
  "Build the project in production mode, and deploy remotely

  `uberjar-aliases` is a string of keywords that should be assembled to build the jar (called with -T)
  `fe-app-name` if the name as found in `shadow-cljs.edn`"
  [{:keys [title errorln]
    :as printers}
   app-dir
   current-task
   uberjar-aliases
   fe-app-name
   env-varname
   target-dir]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (if-let [repo-url (System/getenv env-varname)]
      (let [title-msg "Generate and push uberjar"]
        (title title-msg)
        (-> (build* printers app-dir uberjar-aliases fe-app-name repo-url target-dir)
            (execute-if-success printers
                                target-dir
                                verbose
                                ["git" "push" "--force"]
                                "Push to production"
                                "Error during push"
                                :push)
            (build-cmd/status-to-exit-code printers title-msg)))
      (errorln
       "For security reasons, the repo url should be set as a system environment variable named `"
       env-varname
       "`"))))
