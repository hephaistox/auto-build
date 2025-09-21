(ns auto-build.tasks.copy
  "Copy files from a reference.

  Assume the repository is copied locally, at the right expected commit, under `..` directory."
  (:require
   [auto-build.os.cmd :as    build-cmd
                      :refer [muted]]
   [babashka.fs       :as fs]
   [clojure.edn       :as edn]
   [clojure.string    :as str]))

(defn extract-project
  "Extract the project from the project symbol."
  [app-symbol]
  (->> app-symbol
       str
       (re-find #"(.*)/(.*)")
       last))

(comment
  (extract-project 'com.github.hephaistox/auto-build)
  ;
)

(defn project-dir
  "Turn project name to project directory name."
  [app-name]
  (str/replace app-name #"-" "_"))

(comment
  (project-dir "auto-build")
  ;
)

(defn run
  "Task copying files as defined in `copy-filename`."
  [{:keys [title normalln errorln uri-str subtitle]
    :as _printers}
   copy-filename]
  (when-let [raw-content
             (try (slurp copy-filename)
                  (catch Exception _ (errorln "File" (uri-str copy-filename) "not found") nil))]
    (when-let [content (try (edn/read-string raw-content)
                            (catch Exception e
                              (errorln "File" (uri-str copy-filename) "is not a valid edn")
                              (normalln e)
                              nil))]
      (title "Read" (uri-str copy-filename))
      (doseq [[alias {:keys [files]}] content]
        (let [app-dir (str "../"
                           (-> alias
                               str
                               extract-project
                               project-dir))]
          (subtitle "Copy from alias" alias "in dir" (uri-str app-dir))
          (doseq [file files]
            (let [src-filename (str app-dir "/" file)
                  dst-dir (str (fs/parent file))
                  dst-dir (if (= "" dst-dir) "." dst-dir)]
              (if (= :success
                     (-> (muted ["cp" "-fr" src-filename dst-dir] ".")
                         :status))
                (normalln "Copy file" (uri-str src-filename) "into" (uri-str dst-dir))
                (errorln "Error when copying file" (uri-str src-filename)
                         "into" (uri-str dst-dir))))))))))
