(ns auto-build.tasks.deploy
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.code.formatter :as build-formatter]
            [auto-build.os.exit-codes :as build-exit-codes]
            [auto-build.project.map :as build-project-map]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def tag-formatting
  [["-t" "--tag TAG" "Tag name, e.g. 1.3.2"]
   ["-m" "--message MESSAGE" "Message for tag"]])

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              tag-formatting)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))
;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn deploy
  [{:keys [title errorln], :as printers} app-dir current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [xx (->> (build-project-map/create-project-map app-dir)
                  (build-project-map/add-project-config printers))
          {:keys [project-config-filedesc app-name]} xx]
      (when-not (= :success (get project-config-filedesc :status))
        (errorln "project.edn is not found"))
      (title "Deploy that version" app-name)
      (if (= :success
             (-> (build-formatter/format-clj printers app-dir verbose)
                 :status))
        build-exit-codes/ok
        build-exit-codes/general-errors))))
