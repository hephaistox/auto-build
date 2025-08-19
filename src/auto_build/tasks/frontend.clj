(ns auto-build.tasks.frontend
  (:require
   [auto-build.os.cli-opts    :as build-cli-opts]
   [auto-build.os.cmd         :as    build-cmd
                              :refer [execute-if-success]]
   [auto-build.project.shadow :as build-shadow]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn uberjar*
  [printers app-dir app-alias target-dir repo-url]
  (let [shadow-cljs (build-shadow/read printers app-dir)
        shadow-output-dir (get-in shadow-cljs [:edn :builds (keyword app-alias) :output-dir])]
    (cond-> {:status :success}
      true (execute-if-success printers
                               app-dir
                               verbose
                               ["rm" "-fr" target-dir]
                               "Delete previous build"
                               "Error when deleting previous build"
                               :build-installation)
      true (execute-if-success printers
                               app-dir
                               verbose
                               ["rm" "-fr" shadow-output-dir]
                               "Delete previous js build"
                               "Error when deleting previous js build"
                               :cljs-release-clean)
      true (execute-if-success printers
                               app-dir
                               verbose
                               ["npm" "install"]
                               "Install npm packages"
                               "Error during npm packages installation"
                               :npm-installation)
      app-alias (execute-if-success printers
                                    app-dir
                                    verbose
                                    ["npx" "shadow-cljs" "release" app-alias]
                                    "Create a cljs release"
                                    "Error during creation of a cljs release"
                                    :cljs-release)
      true (execute-if-success printers
                               app-dir
                               verbose
                               ["clojure" "-T:uberjar" ":target-dir" target-dir]
                               "Build the uberjar"
                               "Error during uberjar creation"
                               :uberjar)
      true (execute-if-success printers
                               target-dir
                               verbose
                               ["git" "init" "-b" "master"]
                               "Creates a repo"
                               "Error during repo creation"
                               :create-repo)
      true (execute-if-success printers
                               target-dir
                               verbose
                               ["git" "remote" "add" "clever" repo-url]
                               "Add clever remotes"
                               "Error when adding clever remotes"
                               :add-remote)
      true (execute-if-success printers
                               target-dir
                               verbose
                               ["git" "add" "."]
                               "Add jar to index"
                               "Error when adding jar to index"
                               :add-jar-to-index)
      true (execute-if-success printers
                               target-dir
                               verbose
                               ["git" "commit" "-m" "\"auto\""]
                               "Commit"
                               "Error during commit creation"
                               :commit))))

;; ********************************************************************************
;; *** Task
;; ********************************************************************************

(defn deploy
  [{:keys [title]
    :as printers}
   app-dir
   target-dir
   app-alias
   repo-url]
  (let [title-msg "Deploy to production"]
    (title title-msg)
    (-> (uberjar* printers app-dir app-alias target-dir repo-url)
        (build-cmd/execute-if-success printers
                                      target-dir
                                      verbose
                                      ["git" "push" "--force" "-u" "clever" "master"]
                                      "Push to clever"
                                      "Pushing to clever has failed"
                                      :push-to-clever)
        (build-cmd/status-to-exit-code printers title-msg))))

(defn repl-cljs
  [{:keys [title]
    :as printers}
   app-dir
   app-alias]
  (let [title-msg "Local development mode building"
        execute-if-success (fn [prev-res cmd title concept-kw]
                             (build-cmd/execute-if-success prev-res
                                                           printers
                                                           app-dir
                                                           cmd
                                                           title
                                                           concept-kw
                                                           nil
                                                           verbose))]
    (title title-msg)
    (->
      {:status :success}
      (execute-if-success ["npx" "shadow-cljs" "watch" app-alias] "Watch clojurescript" :watch-cljs)
      (build-cmd/status-to-exit-code printers title-msg))))

(defn uberjar
  [{:keys [title]
    :as printers}
   app-dir
   target-dir
   app-alias
   repo-url]
  (let [title-msg "Production mode building"]
    (title title-msg)
    (-> (uberjar* printers app-dir app-alias target-dir repo-url)
        (build-cmd/status-to-exit-code printers title-msg))))
