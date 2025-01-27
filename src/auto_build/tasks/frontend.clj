(ns auto-build.tasks.frontend
  (:require
   [auto-build.os.cli-opts    :as build-cli-opts]
   [auto-build.os.cmd         :as build-cmd]
   [auto-build.os.exit-codes  :as build-exit-codes]
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

(defn- execute-if-success
  [{previous-status :status
    :as previous-res}
   {:keys [normalln errorln subtitle]
    :as _printers}
   app-dir
   cmd
   subtitle-msg
   concept-kw
   stream-to-res-fn]
  (if (= :success previous-status)
    (do (when (fn? subtitle) (subtitle subtitle-msg))
        (when verbose (normalln "Execute" cmd))
        (let [res (build-cmd/print-on-error cmd app-dir normalln errorln 10 100 100)
              {:keys [status out-stream]} res]
          (merge previous-res
                 {:status status
                  concept-kw res}
                 (if (= status :success)
                   (if (fn? stream-to-res-fn) (stream-to-res-fn status out-stream) {})
                   (do (errorln "Error during" subtitle-msg) {:status :cmd-failed})))))
    (do (subtitle "Skip:" subtitle-msg) (assoc previous-res concept-kw :skipped))))

(defn- status-to-exit-code
  [{:keys [status]
    :as _previous-res}
   {:keys [title-valid title-error]
    :as _printers}
   message]
  (if (= :success status)
    (do (title-valid message) build-exit-codes/ok)
    (do (title-error message "failed") build-exit-codes/general-errors)))

(defn production-mode
  [{:keys [title]
    :as printers}
   app-dir
   app-alias
   repo-url]
  (let [title-msg "Production mode building"
        shadow-cljs (build-shadow/read printers app-dir)
        shadow-output-dir (get-in shadow-cljs [:edn :builds (keyword app-alias) :output-dir])
        target-dir "target/production"
        cmd-if-success (fn [prev-res dir cmd title concept-kw]
                         (execute-if-success prev-res printers dir cmd title concept-kw nil))]
    (title title-msg)
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
        (cmd-if-success app-dir ["clojure" "-T:uberjar"] "Build the uberjar" :uberjar)
        (cmd-if-success target-dir ["git" "init" "-b" "master"] "Creates a repo" :create-repo)
        (cmd-if-success target-dir
                        ["git" "remote" "add" "clever" repo-url]
                        "Creates a repo"
                        :add-remote)
        (cmd-if-success target-dir ["git" "add" "."] "Add jar file to commit" :add-files)
        (cmd-if-success target-dir ["git" "commit" "-m" "\"auto\""] "Commit" :commit)
        (status-to-exit-code printers title-msg))))

(defn dev-mode
  [{:keys [title]
    :as printers}
   app-dir
   app-alias]
  (let [title-msg "Development mode building"
        cmd-if-success (fn [prev-res cmd title concept-kw]
                         (execute-if-success prev-res printers app-dir cmd title concept-kw nil))]
    (title title-msg)
    (-> {:status :success}
        (cmd-if-success ["npx" "shadow-cljs" "watch" app-alias] "Watch clojurescript" :watch-cljs)
        (status-to-exit-code printers title-msg))))

;; (do (println "Build production uberjar")
;;     (shell "npm install ")
;;     (shell "npx shadow-cljs release app")
;;     (shell "clj -T:uberjar")
;;     (shell "git init -b master" {:dir target-dir})
;;     (shell (str "git remote add clever " (System/getenv "SASU_CAUMOND_PROD_REPO"))
;;            {:dir target-dir})
;;     (shell "git add ." {:dir target-dir})
;;     (shell "git commit -m \"auto\"" {:dir target-dir}))
