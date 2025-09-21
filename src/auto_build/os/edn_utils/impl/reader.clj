(ns auto-build.os.edn-utils.impl.reader
  "Read an implementation edn.

  Note: Is apart from write as write needs formatting and formatting needs to read zprintrc."
  (:require
   [auto-build.os.file     :as build-file]
   [auto-build.os.filename :as build-filename]
   [clojure.edn            :as edn]))

(defn read-edn
  [{:keys [errorln uri-str exception-msg]
    :as printers}
   edn-filename]
  (let [filedesc (build-file/read-file printers edn-filename)
        {:keys [raw-content status]} filedesc]
    (merge {:filedesc filedesc
            :status status}
           (when (= :success status)
             (try {:edn (edn/read-string raw-content)}
                  (catch Exception e
                    (when (fn? errorln)
                      (errorln "File"
                               (-> edn-filename
                                   ((if (fn? uri-str) uri-str identity))
                                   build-filename/absolutize)
                               "is not a valid edn.")
                      (when exception-msg (exception-msg (.getMessage e))))
                    {:exception e
                     :status :edn-failed}))))))
