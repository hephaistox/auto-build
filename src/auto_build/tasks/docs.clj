(ns auto-build.tasks.docs
  "See docs: https://github.com/hephaistox/automaton-build/blob/main/src/bb/automaton_build/tasks/docs.clj"
  (:refer-clojure :exclude [format])
  (:require
   [auto-build.code.doc      :as build-doc]
   [auto-build.code.vcs      :as build-vcs]
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [execute-if-success]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [auto-build.os.file       :as build-file]))

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
  [{:keys [subtitle uri-str]
    :as printers}
   app-dir
   current-task
   doc-branch]
  (let [version tag
        doc-dir (build-file/create-temp-dir "")
        pdoc-dir (build-cmd/parameterize doc-dir)
        pversion (build-cmd/parameterize version)]
    (subtitle "Create version" (uri-str tag))
    (-> {:status :success}
        #_(execute-if-success printers
                              app-dir
                              ["git" "status" "-s"]
                              "Is repo is clean?"
                              "Repo is not clean, stop"
                              :git-status
                              (fn [status out-stream]
                                {:status (if (and (= status :success)
                                                  (->> out-stream
                                                       first
                                                       seq))
                                           :dirty-state
                                           :success)})
                              verbose)
        (execute-if-success printers
                            app-dir
                            ["clojure" "-X:codox" ":version" pversion :output-path pdoc-dir]
                            (str "Documentation version " (uri-str version) " is created")
                            (str "Error during creation of documentation version " pversion)
                            :version-creation
                            nil
                            verbose)
        (execute-if-success printers
                            app-dir
                            ["git" "switch" doc-branch]
                            (str "Switch to branch " (uri-str doc-branch))
                            "Error during branch switch"
                            :git-branch-switch
                            nil
                            verbose))
    #_(cond
        exit-code exit-code
        (= :success
           (some-> printers
                   ;; (build-vcs/clean-state app-dir verbose)
                   ;; (when-success? printers "State is clean")
                   ;; (build-doc/build-version app-dir tag tmp-dir verbose)
                   ;; (when-success? printers "Version created")
                   ;; (build-vcs/switch-branch app-dir verbose doc-branch)
                   ;; (when-success? printers (str "On branch" (uri-str doc-branch)))
                   (build-doc/copy-version app-dir tag tmp-dir verbose)
                   (when-success? printers "Version copied")
                   (build-doc/link-to-latest app-dir tag verbose)
                   (when-success? printers "Update latest link")
                   (build-doc/add-to-index app-dir verbose)
                   (when-success? printers "Add changes to index")
                   (build-doc/commit-changes app-dir tag verbose)
                   (when-success? printers "Commit")
                   (build-doc/push-changes app-dir verbose)
                   (when-success? printers "Push")
                   (build-vcs/switch-branch-back app-dir verbose)
                   (final-cmd printers "Switch back to your branch\n" "Doc succesful")))
        build-exit-codes/ok
        :else build-exit-codes/general-errors)))

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
            (-> (docs* printers app-dir current-task doc-branch)
                (build-cmd/status-to-exit-code printers title-msg)))
          (do (build-cli-opts/print-help-message cli-opts current-task)
              build-exit-codes/invalid-argument)))))
