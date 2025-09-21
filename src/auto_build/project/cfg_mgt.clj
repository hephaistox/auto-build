(ns auto-build.project.cfg-mgt
  "Configuration management utilities"
  (:require
   [auto-build.os.cmd :refer [as-string]]
   [babashka.fs       :as fs]
   [clojure.string    :as str]))

(defn is-git-repo?
  "Returns `true` if `dir` is a directory containing a git repo"
  [dir]
  (every? true? ((juxt fs/exists? fs/directory?) (fs/file dir ".git"))))

(defn git-data
  "Returns a map of `git` informations from the repo in `dir`:
  - `:git-statuts`
  - `:actual-branch`
  - `actual-sha`"
  [dir]
  (let [pstatus (-> ["git" "status" "-s"]
                    (as-string dir))
        status (:out-stream pstatus)]
    (when (and (fs/exists? dir) (fs/directory? dir) (is-git-repo? dir))
      {:git-status status
       :app-dir dir
       :actual-branch (-> ["git" "rev-parse" "--abbrev-ref" "HEAD"]
                          (as-string dir)
                          :out-stream
                          first)
       :actual-sha (-> ["git" "rev-parse" "HEAD"]
                       (as-string dir)
                       :out-stream
                       first)})))

(comment
  (git-data "auto_build")
  (-> ["git" "status" "-s"]
      (as-string "landing")
      :out-stream)
  (println "In dir" "landing"
           "These are the status:" (-> ["git" "status" "-s"]
                                       (as-string "landing")
                                       :out-stream))
  ;
)

(defn local-repos
  "List of project directories containing repos.

  A local repo is a subdirectory, that contains a `.git` folder."
  [dir]
  (->> (fs/list-dir dir)
       (filter fs/directory?)
       (map #(let [dir (-> %
                           fs/components
                           last
                           str)]
               dir))
       (filter is-git-repo?)))


(comment
  (local-repos ".")
  ;
)

;; ********************************************************************************
;;
;; ********************************************************************************

(defn extract-project
  "Extract the project from the project symbol."
  [app-symbol]
  (->> app-symbol
       (re-find #"(.*)/(.*)")
       last))

(defn project-dir
  "Extract the project directory from the project symbol."
  [app-name]
  (str/replace app-name #"-" "_"))

(comment
  (extract-project "com.github.hephaistox/auto-web")
  (project-dir "auto-web")
  ;;
)
