(ns auto-build.tasks.deploy
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [analyze-if-success analyze-res execute-if-success]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [clojure.string           :as str]))

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

(defn- msg-tokenize [s] (str "\"" s "" "\""))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn- deploy*
  [{:keys [uri-str normalln]
    :as printers}
   target-branch-name
   app-dir]
  (-> {:status :success}
      (analyze-res printers
                   app-dir
                   verbose
                   ["git" "branch" "--show-current"]
                   "Get current branch" "Couldn't check that branch is main"
                   :git-branch (fn [{:keys [out-stream]}]
                                 (let [branch-name (first out-stream)]
                                   {:branch-name branch-name
                                    :status (if (= target-branch-name branch-name)
                                              :success
                                              (do (normalln "Expect branch" (uri-str
                                                                             target-branch-name)
                                                            "found" (uri-str branch-name))
                                                  :wrong-branch))})))
      (analyze-if-success printers
                          app-dir
                          verbose
                          ["git" "status" "-s"]
                          "Is git status clean?" "Status should be clean"
                          :git-status (fn [{:keys [status out-stream]}]
                                        {:status (if (and (= status :success)
                                                          (->> out-stream
                                                               first
                                                               seq))
                                                   :dirty-state
                                                   :success)}))
      (analyze-if-success
       printers
       app-dir
       verbose
       ["git" "status"]
       "Is branch pushed?"
       (str "Branch " (uri-str target-branch-name) " seems to be async with remote branch")
       :git-status (fn [{:keys [status out-stream]}]
                     {:status (if (and (= status :success)
                                       (->> out-stream
                                            (filter
                                             #(str/includes? % "Your branch is up to date with"))
                                            first
                                            empty?))
                                :not-pushed
                                :success)}))
      (analyze-if-success printers
                          app-dir
                          verbose
                          ["gh" "run" "list"]
                          "List running gh actions" (str "Branch "
                                                         (uri-str target-branch-name)
                                                         " is not validated on github runners.")
                          :gh-run-list
                          (fn [{:keys [out-stream]}]
                            (let [res (last out-stream)]
                              (cond-> {:run-id (->> (str/split res #"\t")
                                                    (drop 6)
                                                    first)}
                                (re-find #"completed\tsuccess" res) (assoc :status :success)
                                (re-find #"completed\tfailure" res) (assoc :status :run-failed)
                                (not (re-find #"completed\t" res)) (assoc :status :wip)))))
      (analyze-if-success
       printers
       app-dir
       verbose
       (if force?
         ["git" "tag" "-f" "-a" tag "-m" (msg-tokenize message)]
         ["git" "tag" "-a" tag "-m" (msg-tokenize message)])
       (str "Create tag " (uri-str tag) " locally")
       (str "Creation of tag " (uri-str tag) " has failed. Use `-f` option to force.")
       :create-tag
       nil)
      (execute-if-success printers
                          app-dir
                          verbose
                          (concat ["git" "push" "origin" tag "--force-with-lease"]
                                  (when force? ["--force"]))
                          (str "Push tag " (uri-str tag))
                          (str "Fail to push tag " (uri-str tag))
                          :tag-push)))

(defn deploy
  [{:keys [errorln title uri-str]
    :as printers}
   app-dir
   target-branch-name
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg (str "Deploy version " (uri-str tag))]
      (title title-msg)
      (if (every? some? [tag message])
        (-> (deploy* printers target-branch-name app-dir)
            (build-cmd/status-to-exit-code printers title-msg))
        (do (when-not tag (errorln "Tag is mandatory"))
            (when-not message (errorln "Message is mandatory"))
            (build-cli-opts/print-help-message cli-opts current-task)
            build-exit-codes/ok)))))
