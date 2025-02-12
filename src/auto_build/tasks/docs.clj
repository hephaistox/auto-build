(ns auto-build.tasks.docs
  "See docs: https://github.com/hephaistox/automaton-build/blob/main/src/bb/automaton_build/tasks/docs.clj"
  (:refer-clojure :exclude [format])
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [analyze-res execute-if-success execute-whateverstatus]]
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
        (analyze-res printers
                     app-dir
                     verbose
                     ["git" "status" "-s"]
                     "Is repo is clean?" "Repo is not clean, stop"
                     :git-status (fn [{:keys [out-stream status]
                                       :as res}]
                                   {:status (cond
                                              (not= status :success) res
                                              (seq out-stream) :dirty-state
                                              :else :success)}))
        (execute-if-success
         printers
         app-dir
         verbose
         ["clojure" "-X:codox" ":version" pversion :output-path (build-cmd/parameterize tmp-dir)]
         (str "Documentation version " (uri-str version) " is created")
         (str "Error during creation of documentation version " pversion)
         :version-creation)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "switch" doc-branch]
                            (str "Switch to branch " (uri-str doc-branch))
                            "Error during branch switch"
                            :git-branch-switch)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["mkdir" "-p" version-target-dir]
                            (str "Creates directory " (uri-str version-target-dir))
                            "Error during directory creation"
                            :version-dir-creation)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["cp" "-fr" tmp-dir version-target-dir]
                            (str "Copy version " (uri-str tmp-dir)
                                 " to " (uri-str version-target-dir))
                            "Error during file copy"
                            :cp-new-version-files)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["rm" "-f" "index.html"]
                            "Remove index.html file"
                            "Error during `index.html` removal"
                            :git-remove-previous)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["ln" "-s" "-F" version "index.html"]
                            (str "Update latest to version " (uri-str version))
                            (str "Error when updating latest to version " (uri-str version))
                            :git-latest-updated)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "add" "."]
                            "Add all modifications to index"
                            "Error when adding all modifications to index"
                            :git-add-to-index)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "commit" "-m" (build-cmd/parameterize message) "--allow-empty"]
                            "Commit"
                            "Error during commit"
                            :git-commit)
        (execute-if-success printers
                            app-dir
                            verbose
                            ["git" "push"]
                            "Push to github"
                            "Error during push"
                            :git-push)
        (execute-whateverstatus printers
                                app-dir
                                verbose
                                ["git" "switch" "-"]
                                "Back to the previous branch"
                                "Error during branch switching"
                                :git-branch-switch-back))))

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
