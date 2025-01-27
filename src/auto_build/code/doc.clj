(ns auto-build.code.doc
  (:require
   [auto-build.os.cmd      :as build-cmd]
   [auto-build.os.file     :as build-file]
   [auto-build.os.filename :as build-filename]
   [clojure.string         :as str]))

(defn build-version
  [{:keys [normalln errorln uri-str]
    :as _printers}
   app-dir
   version
   doc-dir
   verbose]
  (let [cmd ["clojure"
             "-X:codox"
             ":version"
             (build-cmd/parameterize version)
             :output-path
             (build-cmd/parameterize doc-dir)]]
    (if verbose
      (do (normalln "Generated in directory" (uri-str app-dir))
          (build-cmd/printing cmd app-dir normalln errorln 10))
      (build-cmd/print-on-error cmd app-dir normalln errorln 10 1000 1000))))

(defn copy-version
  [{:keys [normalln errorln]
    :as _printers}
   app-dir
   version
   doc-dir
   verbose]
  (let [version-dir (build-filename/create-dir-path app-dir version)
        cmd ["cp" "-fr" doc-dir version-dir]]
    (build-file/ensure-dir-exists version-dir)
    (if verbose
      (build-cmd/printing cmd app-dir normalln errorln 10)
      (build-cmd/print-on-error cmd app-dir normalln errorln 10 1000 1000))))

(defn link-to-latest
  [{:keys [normalln errorln]
    :as _printers}
   app-dir
   version
   verbose]
  (let [version-dir (build-filename/create-dir-path app-dir version)
        cmd ["ln" "-s" "-F" version "index.html"]]
    (build-file/ensure-dir-exists version-dir)
    (if verbose
      (build-cmd/printing cmd app-dir normalln errorln 10)
      (build-cmd/print-on-error cmd app-dir normalln errorln 10 1000 1000))))

(defn add-to-index
  [{:keys [normalln errorln]
    :as _printers}
   app-dir
   verbose]
  (let [cmd ["git" "add" "."]]
    (if verbose
      (build-cmd/printing cmd app-dir normalln errorln 10)
      (build-cmd/print-on-error cmd app-dir normalln errorln 10 1000 1000))))

(defn commit-changes
  [{:keys [normalln]
    :as _printers}
   app-dir
   version
   verbose]
  (let [cmd ["git" "commit" "-m" version]
        _ (when verbose (normalln "Execute" cmd))
        res (-> cmd
                (build-cmd/as-string app-dir))
        {:keys [status out-stream]} res]
    {:status (if (or (= :success status) (str/includes? out-stream "nothing to commit"))
               :success
               :commit-failed)
     :commit-res res}))

(defn push-changes
  [{:keys [normalln errorln]
    :as _printers}
   app-dir
   verbose]
  (let [cmd ["git" "push"]]
    (if verbose
      (build-cmd/printing cmd app-dir normalln errorln 10)
      (build-cmd/print-on-error cmd app-dir normalln errorln 10 1000 1000))))
