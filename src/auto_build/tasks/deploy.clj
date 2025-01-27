(ns auto-build.tasks.deploy
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [execute-if-success]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [auto-build.project.map   :as build-project-map]
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
  [{:keys [uri-str]
    :as printers}
   target-branch-name
   app-dir]
  (let [execute-if-success* (fn [res cmd stream-tot-res-fn concept-kw subtitle-msg error-msg]
                              (execute-if-success res
                                                  printers
                                                  app-dir
                                                  verbose
                                                  cmd
                                                  stream-tot-res-fn
                                                  concept-kw
                                                  subtitle-msg
                                                  error-msg))]
    (-> {:status :success}
        (execute-if-success* ["git" "branch" "--show-current"]
                             (fn [_status out-stream]
                               (let [branch-name (first out-stream)]
                                 {:branch-name branch-name
                                  :status
                                  (if (= target-branch-name branch-name) :success :wrong-branch)}))
                             :git-branch
                             "Get current branch"
                             "Couldn't check that branch is main")
        (execute-if-success* ["git" "status" "-s"]
                             (fn [status out-stream]
                               {:status (if (and (= status :success)
                                                 (->> out-stream
                                                      first
                                                      seq))
                                          :dirty-state
                                          :success)})
                             :git-status
                             "Is git status clean?"
                             "Status should be clean")
        (execute-if-success*
         ["git" "status"]
         (fn [status out-stream]
           {:status (if (and (= status :success)
                             (->> out-stream
                                  (filter #(str/includes? % "Your branch is up to date with"))
                                  first
                                  empty?))
                      :not-pushed
                      :success)})
         :git-status
         "Is branch pushed?"
         (str "Branch " (uri-str target-branch-name) " seems to be async"))
        (execute-if-success* ["gh" "run" "list"]
                             (fn [_status out-stream]
                               {:last-run
                                (let [res (last out-stream)]
                                  (cond-> {:run-id (->> (str/split res #"\t")
                                                        (drop 6)
                                                        first)}
                                    (re-find #"completed\tsuccess" res) (assoc :status :success)
                                    (re-find #"completed\tfailure" res) (assoc :status :run-failed)
                                    (not (re-find #"completed\t" res)) (assoc :status :wip)))})
                             :gh-run-list
                             "List running gh actions"
                             (str "Branch " (uri-str target-branch-name) " seems to be async"))
        (execute-if-success*
         (if force?
           ["git" "tag" "-f" "-a" tag "-m" (msg-tokenize "zae")]
           ["git" "tag" "-a" tag "-m" (msg-tokenize "zeaze")])
         (fn [_status out-stream]
           {:last-run (let [res (last out-stream)]
                        (cond-> {:run-id (->> (str/split res #"\t")
                                              (drop 6)
                                              first)}
                          (re-find #"completed\tsuccess" res) (assoc :status :success)
                          (re-find #"completed\tfailure" res) (assoc :status :run-failed)
                          (not (re-find #"completed\t" res)) (assoc :status :wip)))})
         :create-tag
         (str "Create tag " (uri-str tag) " locally")
         (str "Creation of tag " (uri-str tag) " has failed. Use `-f` option to force."))
        (execute-if-success* (concat ["git" "push" "origin" tag "--force-with-lease"]
                                     (when force? ["--force"]))
                             (fn [_status _out-stream] {})
                             :tag-push
                             (str "Push tag " (uri-str tag))
                             (str "Fail to push tag " (uri-str tag))))))

(defn deploy
  [{:keys [errorln title uri-str]
    :as printers}
   app-dir
   target-branch-name
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [tag (get-in cli-opts [:options :tag])
          project-map (->> (build-project-map/create-project-map app-dir)
                           (build-project-map/add-project-config printers))
          {:keys [app-name]} project-map
          message (get-in cli-opts [:options :message])
          title-msg (str "Deploy " (uri-str app-name) " version " (uri-str tag))]
      (title title-msg)
      (if (every? some? [tag message])
        (-> (deploy* printers target-branch-name app-dir)
            (build-cmd/status-to-exit-code printers title-msg))
        (do (when-not tag (errorln "Tag is mandatory"))
            (when-not message (errorln "Message is mandatory"))
            (build-cli-opts/print-help-message cli-opts current-task)
            build-exit-codes/ok)))))
