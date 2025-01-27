(ns auto-build.tasks.frontend
  (:require
   [auto-build.os.cli-opts    :as build-cli-opts]
   [auto-build.os.cmd         :as build-cmd]
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
        shadow-output-dir (get-in shadow-cljs [:edn :builds (keyword app-alias) :output-dir])
        cmd-if-success
        (fn [prev-res dir cmd title concept-kw]
          (build-cmd/execute-if-success prev-res printers dir cmd title concept-kw nil verbose))]
    (-> {:status :success}
        (cmd-if-success app-dir ["rm" "-fr" target-dir] "Delete previous build" :build-installation)
        (cmd-if-success app-dir
                        ["rm" "-fr" shadow-output-dir]
                        "Delete previous js build"
                        :cljs-release-clean)
        (cmd-if-success app-dir ["npm" "install"] "Install npm packages" :npm-installation)
        (cmd-if-success app-dir
                        ["npx" "shadow-cljs" "release" app-alias]
                        "Create a cljs release"
                        :cljs-release)
        (cmd-if-success app-dir
                        ["clojure" "-T:uberjar" ":target-dir" target-dir]
                        "Build the uberjar"
                        :uberjar)
        (cmd-if-success target-dir ["git" "init" "-b" "master"] "Creates a repo" :create-repo)
        (cmd-if-success target-dir
                        ["git" "remote" "add" "clever" repo-url]
                        "Creates a repo"
                        :add-remote)
        (cmd-if-success target-dir ["git" "add" "."] "Add jar file to commit" :add-files)
        (cmd-if-success target-dir ["git" "commit" "-m" "\"auto\""] "Commit" :commit))))

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
                                      ["git" "push" "--force" "-u" "clever" "master"]
                                      "Push to clever"
                                      :push-to-clever
                                      nil
                                      verbose)
        (build-cmd/status-to-exit-code printers title-msg))))

(defn repl-cljs
  [{:keys [title]
    :as printers}
   app-dir
   app-alias]
  (let [title-msg "Local development mode building"
        cmd-if-success (fn [prev-res cmd title concept-kw]
                         (build-cmd/execute-if-success prev-res
                                                       printers
                                                       app-dir
                                                       cmd
                                                       title
                                                       concept-kw
                                                       nil
                                                       verbose))]
    (title title-msg)
    (-> {:status :success}
        (cmd-if-success ["npx" "shadow-cljs" "watch" app-alias] "Watch clojurescript" :watch-cljs)
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
