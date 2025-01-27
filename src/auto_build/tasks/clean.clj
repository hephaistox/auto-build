(ns auto-build.tasks.clean
  "Clean some directories or files"
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as    build-cmd
                           :refer [execute-if-success]]
   [clojure.string         :as str]))

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

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn clean*
  "Remove file from `filepaths` and directories from `dirs`"
  [{:keys [uri-str]
    :as printers}
   app-dir
   filepaths
   dirs]
  (-> {:status :success}
      (execute-if-success printers
                          app-dir
                          verbose
                          (reduce #(conj %1 %2) ["rm" "-f"] filepaths)
                          (->> filepaths
                               (map uri-str)
                               (str/join ", ")
                               (str "Remove files "))
                          "File deletion has failed"
                          :remove-files
                          nil)
      (execute-if-success printers
                          app-dir
                          verbose
                          (reduce #(conj %1 %2) ["rm" "-fr"] dirs)
                          (->> dirs
                               (map uri-str)
                               (str/join ", ")
                               (str "Remove dirs "))
                          "Dir deletion has failed"
                          :remove-dirs
                          nil)))

(defn clean
  "Remove file from `filepaths` and directories from `dirs`"
  [{:keys [title]
    :as printers}
   app-dir
   filepaths
   dirs
   current-task]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Cleaning temporary files"]
      (title title-msg)
      (-> (clean* printers app-dir filepaths dirs)
          (build-cmd/status-to-exit-code printers title-msg)))))
