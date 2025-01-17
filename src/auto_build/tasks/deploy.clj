(ns auto-build.tasks.deploy
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.code.vcs :as build-vcs]
            [auto-build.os.exit-codes :as build-exit-codes]
            [auto-build.os.cmd :refer [when-success?]]
            [auto-build.project.map :as build-project-map]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              [["-t" "--tag TAG" "Tag name, e.g. 1.3.2"]
               ["-f" "--force" "Force the tag push to replace"]
               ["-m" "--message MESSAGE" "Message for tag"]])
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

(def force? (get-in cli-opts [:options :force]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn- print-run-message
  [{:keys [normalln], :as printers} app-dir run-id]
  (let [res (build-vcs/gh-run-view printers app-dir verbose run-id)
        {:keys [message]} res]
    (normalln message)))

(defn- do-tag
  [{:keys [subtitle uri-str], :as printers} app-dir]
  (subtitle "Tag" tag "with message" (uri-str message))
  (some-> (build-vcs/tag printers app-dir verbose tag message)
          (when-success? printers "Tagged is assigned")
          (build-vcs/push-tag app-dir verbose tag force?)))

(defn- deploy*
  [{:keys [title errorln normalln uri-str], :as printers} app-dir]
  (let [uri-str (if (fn? uri-str) uri-str identity)
        project-map (->> (build-project-map/create-project-map app-dir)
                         (build-project-map/add-project-config printers))
        {:keys [app-name]} project-map
        branch-name (-> (build-vcs/current-branch printers "" false)
                        :branch-name)]
    (title "Deploy" (uri-str app-name) "version" tag)
    (if (= :main branch-name)
      (let [run-wip (-> (build-vcs/clean-state printers app-dir verbose)
                        (when-success? printers "State is clean")
                        (build-vcs/nothing-to-push app-dir verbose)
                        (when-success? printers "Remote branch is uptodate")
                        (build-vcs/gh-run-wip? app-dir verbose))
            {:keys [status last-run]} run-wip]
        (case status
          :success (do (normalln "Commit is validated on github!")
                       (do-tag printers app-dir))
          :run-failed (do
                        (errorln "Commit is not validated")
                        (print-run-message printers app-dir (:run-id last-run))
                        build-exit-codes/invalid-state)
          :wip (do (errorln "Commit validation is still in progress.")
                   (print-run-message printers app-dir (:run-id last-run))
                   build-exit-codes/invalid-state)
          :dirty-state (do (errorln "Your local state is not clean.")
                           build-exit-codes/invalid-state)
          :not-pushed (do (errorln "Remote repo is not updated")
                          build-exit-codes/invalid-state)
          (do (errorln "Unexpected clause" status)
              build-exit-codes/general-errors)))
      (do (errorln "branch should be main, found" (uri-str branch-name))
          build-exit-codes/general-errors))))

(defn deploy
  [{:keys [errorln], :as printers} app-dir current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [tag (get-in cli-opts [:options :tag])
          message (get-in cli-opts [:options :message])]
      (if (every? some? [tag message])
        (deploy* printers app-dir)
        (do (when-not tag (errorln "Tag is mandatory"))
            (when-not message (errorln "Message is mandatory"))
            (build-cli-opts/print-help-message cli-opts
                                               (:name current-task)))))))
