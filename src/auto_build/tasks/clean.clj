(ns auto-build.tasks.clean
  "Clean some directories or files"
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [when-success?]]
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
               ["-m" "--message MESSAGE" "Message for tag"]])
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn- clean-file
  [{:keys [normalln errorln]
    :as _printers}
   dir
   filepaths]
  (when verbose (normalln "Remove" (str/join "," filepaths)))
  (build-cmd/print-on-error (reduce #(conj %1 %2) ["rm" "-f"] filepaths)
                            dir
                            normalln
                            errorln
                            10
                            100
                            100))

(defn- clean-dirs
  [{:keys [normalln errorln]
    :as _printers}
   dir
   dirs]
  (when verbose (normalln "Remove" (str/join "," dirs)))
  (build-cmd/print-on-error (reduce #(conj %1 %2) ["rm" "-fr"] dirs)
                            dir
                            normalln
                            errorln
                            10
                            100
                            100))

(defn clean
  "Remove file from `filepaths` and directories from `dirs`"
  [printers dir filepaths dirs]
  (if (= :success
         (some-> printers
                 (clean-file dir filepaths)
                 (when-success? printers nil)
                 (clean-dirs dir dirs)
                 :status))
    build-exit-codes/ok
    build-exit-codes/general-errors))
