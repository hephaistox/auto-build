(ns auto-build.tasks.docs
  "See docs: https://github.com/hephaistox/automaton-build/blob/main/src/bb/automaton_build/tasks/docs.clj"
  (:refer-clojure :exclude [format])
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [execute-if-success]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [auto-build.os.file       :as build-file]
   [auto-build.os.filename   :as build-filename]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              [["-t" "--tag TAG" "Tag name, e.g. 1.3.2"]
               ["-m" "--message MESSAGE" "Message for tag"]])
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn docs*
  "Generate docs on branch `doc-branch`"
  [{:keys [uri-str]
    :as printers}
   app-dir
   doc-branch]
  (let [version tag
        pversion (build-cmd/parameterize version)
        tmp-dir (build-file/create-temp-dir version)
        version-target-dir (build-filename/create-dir-path app-dir version)]
    (-> {:status :success}
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "status" "-s"]
                            "Is repo is clean?" "Repo is not clean, stop"
                            :git-status (fn [status out-stream]
                                          {:status (if (and (= status :success)
                                                            (->> out-stream
                                                                 first
                                                                 seq))
                                                     :dirty-state
                                                     :success)}))
        (execute-if-success
         printers
         app-dir
         verbose
         ["clojure" "-X:codox" ":version" pversion :output-path (build-cmd/parameterize tmp-dir)]
         (str "Documentation version " (uri-str version) " is created")
         (str "Error during creation of documentation version " pversion)
         :version-creation
         nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "switch" doc-branch]
                            (str "Switch to branch " (uri-str doc-branch))
                            "Error during branch switch"
                            :git-branch-switch
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["mkdir" "-p" version-target-dir]
                            (str "Creates directory " (uri-str version-target-dir))
                            "Error during directory creation"
                            :version-dir-creation
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["cp" "-fr" tmp-dir version-target-dir]
                            (str "Copy version " (uri-str tmp-dir)
                                 " to " (uri-str version-target-dir))
                            :cp-new-version-files
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["rm" "-f" "index.html"]
                            "Remove index.html file"
                            :git-remove-previous
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["ln" "-s" "-F" version "index.html"]
                            (str "Update latest to version " (uri-str version))
                            :git-latest-updated
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "add" "."]
                            "Add to index"
                            :git-add-to-index
                            nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "commit" "-m" (build-cmd/parameterize message)]
                            "Add to index"
                            :git-commit
                            nil)
        (execute-if-success printers app-dir verbose ["git" "push"] "Push to github" :git-push nil)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "switch" "-"]
                            "Back to the previous branch" "Error during branch switching"
                            :git-branch-switch-back nil))))

(defn docs
  "Generate docs on branch `doc-branch`"
  [{:keys [title errorln]
    :as printers}
   app-dir
   current-task
   doc-branch]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (do (when (nil? tag) (errorln "Tag is mandatory"))
        (when (nil? message) (errorln "Message is mandatory"))
        (if (every? some? [tag message])
          (let [title-msg "Generate docs"]
            (title title-msg)
            (-> (docs* printers app-dir doc-branch)
                (build-cmd/status-to-exit-code printers title-msg)))
          (do (build-cli-opts/print-help-message cli-opts current-task)
              build-exit-codes/invalid-argument)))))
