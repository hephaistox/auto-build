(ns auto-build.tasks.uberjar
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as    build-cmd
                           :refer [execute-if-success]]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn uberjar*
  [printers app-dir target-dir]
  (-> {:status :success}
      (execute-if-success printers
                          app-dir
                          verbose
                          ["rm" "-fr" target-dir]
                          "Delete previous build"
                          "Error when deleting previous build"
                          :build-installation)
      (execute-if-success printers
                          app-dir
                          verbose
                          ["clojure" "-T:build" "compile-jar" ":target-dir" target-dir]
                          "Build the uberjar"
                          "Error during uberjar creation"
                          :uberjar)
      (execute-if-success printers
                          target-dir
                          verbose
                          ["git" "init" "-b" "master"]
                          "Creates a repo"
                          "Error during repo creation"
                          :create-repo)
      (execute-if-success printers
                          target-dir
                          verbose
                          ["git" "add" "."]
                          "Add jar to index"
                          "Error when adding jar to index"
                          :add-jar-to-index)
      (execute-if-success printers
                          target-dir
                          verbose
                          ["git" "commit" "-m" "\"auto\""]
                          "Commit"
                          "Error during commit creation"
                          :commit)))

;; ********************************************************************************
;; *** Task
;; ********************************************************************************

(defn uberjar
  [{:keys [title]
    :as printers}
   app-dir
   target-dir]
  (let [title-msg "Production mode building"]
    (title title-msg)
    (-> (uberjar* printers app-dir target-dir)
        (build-cmd/status-to-exit-code printers title-msg))))
