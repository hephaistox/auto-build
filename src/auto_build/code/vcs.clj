(ns auto-build.code.vcs
  "Version Control System.

  Proxy to [git](https://git-scm.com/book/en/v2)."
  (:require [auto-build.os.file :as build-file]
            [auto-build.os.filename :as build-filename]
            [auto-build.os.cmd :as build-cmd]
            [clojure.string :as str]))

;; ********************************************************************************
;; Private
;; ********************************************************************************
(defn- execute-vcs-cmd
  [{:keys [normalln errorln], :as _printers} app-dir verbose cmd concept-kw
   error-msg out-stream-fn]
  (when verbose (normalln "Execute" cmd))
  (let [res (build-cmd/as-string cmd app-dir 100 100)
        {:keys [status out-stream err-stream]} res]
    (merge {:status status, concept-kw res}
           (if (= status :success)
             (out-stream-fn status out-stream)
             (do (errorln error-msg)
                 (when-not (empty? out-stream) (apply normalln out-stream))
                 (when-not (empty? err-stream) (apply normalln err-stream))
                 {:status :cmd-failed})))))

(defn- msg-tokenize [s] (str "\"" s "" "\""))

;; ********************************************************************************
;; API
;; ********************************************************************************
(defn latest-commit-message
  "Returns a map with :tag key."
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "log" "-1" "--pretty=format:%B"]
                   :git-describe
                   "Error when getting commit message"
                   (fn [_status out-stream] {:tag (first out-stream)})))

(defn latest-commit-sha
  "Returns a map with :commit-sha key."
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "log" "-n" "1" "--pretty=format:%H"]
                   :git-describe
                   "Error when getting commit message"
                   (fn [_status out-stream] {:commit-sha (first out-stream)})))

(defn current-branch
  "Returns a map with :branch-name key"
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "branch" "--show-current"]
                   :git-branch
                   "Error when getting branch"
                   (fn [_status out-stream] {:branch-name (first out-stream)})))

(defn switch-branch
  "Switch to branch `branch-name`"
  [printers app-dir verbose branch-name]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "switch" branch-name]
                   :git-branch-switch
                   "Error when switching branch"
                   (fn [_status _out-stream] {})))

(defn switch-branch-back
  "Returns to previous branch"
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "switch" "-"]
                   :git-branch-switch
                   "Error when switching branch"
                   (fn [_status _out-stream] {})))

(defn clean-hard
  "Returns a command to get the name of the current branch."
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "clean" "-fqdxi"]
                   :git-clean
                   "Error when cleaning git repo"
                   (fn [_status _out-stream] {})))

(defn tag
  "Creates a tag under name `version` and message `tag-msg`."
  [printers app-dir verbose version tag-msg force?]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   (if force?
                     ["git" "tag" "-f" "-a" version "-m" (msg-tokenize tag-msg)]
                     ["git" "tag" "-a" version "-m" (msg-tokenize tag-msg)])
                   :git-tag-create
                   "Error when creating tags"
                   (fn [_status _out-stream] {})))

(defn push-tag
  [printers app-dir verbose version force?]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   (concat ["git" "push" "origin" version "--force-with-lease"]
                           (when force? ["--force"]))
                   :git-push
                   "Error when pushing tags"
                   (fn [_status _out-stream] {})))

(defn find-git-repo
  "Search a git repo in `dir` of its parent directories."
  [dir]
  (build-file/search-in-parents dir ".git"))

(defn spit-hook
  "Spit the `content` in the hook called `hook-name` of `app-dir` repo."
  [printers app-dir hook-name content]
  (let [hook-filename
          (build-filename/create-file-path app-dir ".git" "hooks" hook-name)]
    (build-file/write-file hook-filename printers content)
    (-> (build-file/make-executable hook-filename)
        str)))

(defn git-setup-dir
  "Returns the hidden `.git` directory of app in repo `repo-dir`"
  [repo-dir]
  (build-filename/create-dir-path repo-dir ".git"))

(defn clean-state
  "Returns `status` to `success` if clean, `dirty-state`"
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "status" "-s"]
                   :git-status
                   "Error when getting git status"
                   (fn [status out-stream]
                     {:status (if (and (= status :success)
                                       (->> out-stream
                                            first
                                            seq))
                                :dirty-state
                                :success)})))

(defn nothing-to-push
  "Returns `status` to `success` if nothing pushed, `not-pushed`"
  [printers app-dir verbose]
  (execute-vcs-cmd
    printers
    app-dir
    verbose
    ["git" "status"]
    :git-status
    "Error when getting git status"
    (fn [status out-stream]
      {:status (if (and (= status :success)
                        (->> out-stream
                             (filter #(str/includes?
                                        %
                                        "Your branch is up to date with"))
                             first
                             empty?))
                 :not-pushed
                 :success)})))

(defn current-tag
  "Returns a command to detect clean state"
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "describe" "--exact-match" "--tags"]
                   :git-describe
                   "Error when getting git tag"
                   (fn [_status out-stream] {:tag (first out-stream)})))

(defn current-repo-url
  "Command to return the current remote url"
  [printers app-dir verbose]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["git" "remote" "-v"]
                   :git-remote
                   "Error when getting repo url"
                   (fn [_status out-stream]
                     {:repo-url (->> out-stream
                                     (map (fn [line]
                                            (->> line
                                                 (re-find
                                                   #"origin\s*([^\s]*).*(push)")
                                                 second)))
                                     (filter some?)
                                     first)})))

(defn gh-run-wip?
  "Returns  if the last workflow is in progress"
  [printers app-dir verbose]
  (let [res (execute-vcs-cmd printers
                             app-dir
                             verbose
                             ["gh" "run" "list"]
                             :gh-run-list
                             "Error when getting github run list"
                             (fn [_status out-stream]
                               {:last-run
                                  (let [res (last out-stream)]
                                    (cond-> {:run-id (->> (str/split res #"\t")
                                                          (drop 6)
                                                          first)}
                                      (re-find #"completed\tsuccess" res)
                                        (assoc :status :success)
                                      (re-find #"completed\tfailure" res)
                                        (assoc :status :run-failed)
                                      (not (re-find #"completed\t" res))
                                        (assoc :status :wip)))}))]
    (assoc res :status (get-in res [:last-run :status]))))

(defn gh-run-view
  "Returns the view of run-id"
  [printers app-dir verbose run-id]
  (execute-vcs-cmd printers
                   app-dir
                   verbose
                   ["gh" "run" "view" run-id]
                   :gh-run-view
                   "Error when getting github view"
                   (fn [_status out-stream]
                     {:run-id run-id, :message (first out-stream)})))
