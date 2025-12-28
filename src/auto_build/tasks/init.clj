(ns auto-build.tasks.init
  "Initialzation of a repo, do preparation tasks:

  - Add wiki repo in `docs/wiki`"
  (:require
   [auto-build.os.cmd :as    build-cmd
                      :refer [printing]]))

(defn run
  [{:keys [title errorln normalln]
    :as _printers}
   wiki-repo]
  (title "Build wiki repo")
  (printing ["git" "clone" wiki-repo "docs/wiki"] "." normalln errorln 10))
