(ns auto-build.os.file
  "Tools to manipulate local files

  Is a proxy to babashka.fs tools."
  (:require
   [auto-build.os.filename :as build-filename]
   [babashka.fs            :as fs]
   [clojure.pprint         :as pp]
   [clojure.string         :as str]))

;; ********************************************************************************
;; Directory manipulation

(defn is-existing-dir?
  "Returns `dirpath` if this the path exists and is a directory."
  [dirpath]
  (if (str/blank? dirpath) "." (when (and (fs/directory? dirpath) (fs/exists? dirpath)) dirpath)))

(defn ensure-dir-exists
  "Creates directory `dirpath` if not already existing.
  Return `dirpath` if succesful, `nil` if creation has failed "
  [dirpath]
  (when (string? dirpath) (when-not (is-existing-dir? dirpath) (fs/create-dirs dirpath)) dirpath))

(defn delete-dir
  "Deletes the directory at `dirpath` and returns `dirpath` if deleted. Returns `nil` otherwise"
  [dirpath]
  (when (is-existing-dir? dirpath) (fs/delete-tree dirpath) dirpath))

(defn ensure-empty-dir
  "Ensure the directory `dirpath` is existing and empty."
  [dirpath]
  (delete-dir dirpath)
  (ensure-dir-exists dirpath))

(defn copy-dir
  ([src-path dst-path] (fs/copy-tree src-path dst-path) nil)
  ([src-path dst-path options] (fs/copy-tree src-path dst-path options) nil))

;; ********************************************************************************
;; File manipulation

(defn expand-home-str
  "Return `path` where `~` is expanded to the actual value of the current home directory."
  [path]
  (-> path
      fs/path
      fs/expand-home
      clojure.core/str))

(defn is-existing-file?
  "Returns the `filepath` if it already exists and is a regular file. Returns `nil` otherwise."
  [filepath]
  (if (or (nil? filepath) (and (string? filepath) (str/blank? filepath)))
    "."
    (when (and (fs/exists? filepath) (fs/regular-file? filepath)) filepath)))

(defn delete-file
  "Deletes `filepath` and returns it.
   If `filepath` does not exist, returns nil."
  [filepath]
  (when (is-existing-file? filepath) (fs/delete filepath) filepath))

(defn copy-file
  "Copy `src-filepath` to `dst-path`, that could be a filepath, or a directory where the file will be stored.
  Returns `nil`"
  [src-filepath dst-path options]
  (fs/copy src-filepath dst-path options)
  nil)

(defn make-executable
  "Make file `filepath` executable.
  Returns `filepath`"
  [filepath]
  (when (is-existing-file? filepath)
    (fs/set-posix-file-permissions filepath (fs/str->posix "rwx------"))
    filepath))

(defn create-sym-link
  "Creates a sym link to `target` linking to `filepath`.
  Returns `filepath`"
  [filepath target]
  (fs/create-sym-link filepath target)
  filepath)

;; ********************************************************************************
;; Path manipulation

(defn is-existing-path?
  "Returns true if `path` path already exist."
  [path]
  (if (str/blank? path) "." (when-not (str/blank? path) (when (fs/exists? path) path))))

(defn modified-since
  "Returns `true` if `anchor-filepath` is older than one of the file in `file-set`."
  [anchor-filepath file-set]
  (let [file-set (filter some? file-set)]
    (when anchor-filepath (seq (fs/modified-since anchor-filepath file-set)))))

(defn delete-path
  "Deletes `path` and returns it.
   Returns nil if the `path` does not exists."
  [path]
  (if (fs/directory? path) (delete-dir path) (delete-file path)))

(defn path-on-disk
  "Returns a map with informations on the existance of `path` as found on the disk.
  As all `path` functions, it could be a file or a directory.

  It returns:
  * `:path` given path
  * `:apath` the absolute path
  * `:type` could be `:file` `:directory`
  * `:exist?`"
  [path]
  (cond-> {:path path
           :apath (build-filename/absolutize path)
           :type (if (fs/regular-file? path) :file :directory)}
    (fs/exists? path) (assoc :exist? true)))

;; ********************************************************************************
;; Temporaries

(defn create-temp-file
  "Create a temporary file.
  - The file is stored in the system temporary directory.
  - The file is suffixed with `filepath` (optional)."
  ([] (create-temp-file "tmp"))
  ([filepath]
   (-> (fs/create-temp-file {:suffix (apply str filepath)})
       str)))

(defn create-temp-dir
  "Returns a subdirectory path string of the system temporary directory.
  `sub-dir` is an optional string, each one is a sub directory."
  ([] (create-temp-dir "tmp"))
  ([sub-dir]
   (let [tmp-dir (apply build-filename/create-dir-path
                        (-> (fs/create-temp-dir)
                            str)
                        (interpose build-filename/directory-separator sub-dir))]
     (ensure-dir-exists tmp-dir)
     tmp-dir)))

;; ********************************************************************************
;; Modify file content

(defn read-file
  "Read the file named `filepath`.

Returns:
  * `:filepath` as given as a parameter
  * `:afilepath` file with absolute path
  * `:status` is `:success` or `:file-loading-fail`
  * `:raw-content` (only if `:status` is `:success`)
  * `:exception` (only if `:status` is `:file-loading-fail`)

  That functions print on the cli:
  * nothing if successful or if printers are nil
  * an error message and the message of the exception if the file can't be read."
  [{:keys [errorln uri-str exception-msg]
    :as _printers}
   filepath]
  (let [filepath (str filepath)]
    (merge {:filepath filepath
            :afilepath (build-filename/absolutize filepath)}
           (try {:raw-content (slurp filepath)
                 :status :success}
                (catch Exception e
                  (when (fn? errorln)
                    (errorln "Impossible to load file"
                             ((if (fn? uri-str) uri-str identity) filepath)))
                  (when exception-msg (exception-msg e))
                  {:exception e
                   :status :file-loading-fail})))))

(defn write-file
  "Write the `content` in the file at `filepath` .

  Returns
  * `:filepath` as given as a parameter
  * `:afilepath` file with absolute path
  * `:status` is `:success` or `:file-writing-fail`
  * `:raw-content` if file can be read.
  * `:exception` (only if `:status` is `:file-writing-fail`)"
  [filepath
   {:keys [errorln exception-msg normalln uri-str]
    :as _printers}
   content]
  (merge
   {:filepath filepath
    :afilepath (build-filename/absolutize filepath)
    :raw-content content}
   (try (spit filepath content)
        (when (fn? normalln)
          (normalln "File" ((if (fn? uri-str) uri-str identity) filepath) "is updated"))
        {:status :success}
        (catch Exception e
          (when (fn? errorln)
            (errorln "File" ((if (fn? uri-str) uri-str identity) filepath) "could not be written"))
          (when (fn? exception-msg) (exception-msg e))
          {:exception e
           :status :file-writing-fail}))))

(defn combine-files
  "Read text content of files `src-filepathes` - in the order of the sequence - and combine them into `target-filepath`."
  [printers target-filepath & src-filepathes]
  (->> (map (comp :raw-content read-file) src-filepathes)
       (apply str)
       (write-file printers target-filepath)))

(defn pp-file
  "Pretty print content `file-content` into file `filepath`."
  [filepath file-content]
  (->> (with-out-str (pp/pprint file-content))
       (spit filepath)))

;; ********************************************************************************
;; Search

(defn search-files
  "Search files and dirs.
  * `dirpath` is where the root directory of the search-files
  * `pattern` is a regular expression or a glob as described in [java doc](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  * `options` (Optional, default = {}) are boolean value for `:hidden`, `:recursive` and `:follow-lins`. See [babashka fs](https://github.com/babashka/fs/blob/master/API.md#glob) for details.
  For instance:
  * `(files/search-files \"\" \"**{.clj,.cljs,.cljc,.edn}\")` search all clj file-paths in pwd directory"
  ([dirpath pattern options]
   (let [dirpath (if (str/blank? dirpath) "." dirpath)]
     (when (is-existing-dir? dirpath)
       (mapv str
             (fs/glob dirpath
                      pattern
                      (merge {:hidden true
                              :recursive true
                              :follow-links true}
                             options))))))
  ([dirpath pattern] (search-files dirpath pattern {})))

(defn matching-files
  "Match files recursively found in `dirpath` that are matching `file-pattern`."
  [dirpath file-pattern]
  (fs/glob dirpath file-pattern))

(defn search-in-parents
  "Search `file-or-dir` in the parents directories of `dirpath`."
  [dirpath file-or-dir]
  (loop [dirpath (build-filename/absolutize dirpath)]
    (let [file-candidate (build-filename/create-file-path (str dirpath) file-or-dir)]
      (if (fs/exists? file-candidate)
        dirpath
        (when-not (str/blank? dirpath) (recur (str (fs/parent dirpath))))))))

;; ********************************************************************************
;; Copy

(defn copy-action
  "Copy a file at `path` in a subdir of `src-dirpath` to `dst-dirpath`.

The `options` could be `:replace-existing` `:copy-attributes`. Returns:
  * `:path` given path
  * `:apath` the absolute path
  * `:type` could be `:file` `:directory`
  * `:exist?`
  * `:dst-dirpath`
  * `:options`
  * `:relative-path`
  * `:target-path`"
  ([path src-dirpath dst-dirpath options]
   (let [rp (build-filename/relativize path src-dirpath)
         path-on-disk (path-on-disk path)
         {:keys [file?]} path-on-disk]
     (assoc path-on-disk
            :dst-dirpath dst-dirpath
            :src-dirpath src-dirpath
            :options (merge {:replace-existing true
                             :copy-attributes true}
                            options)
            :relative-path rp
            :target-path (cond->> rp
                           file? build-filename/extract-path
                           :else (build-filename/create-file-path dst-dirpath)))))
  ([path src-dirpath dst-dirpath] (copy-action path src-dirpath dst-dirpath {})))

(defn do-copy-action
  "Do the actual copy of `copy-action`, enrich it with `:status` (`:copy-fail` or `:success`)"
  [{:keys [type path dst-dirpath target-path options exist?]
    :as copy-action}]
  (ensure-dir-exists dst-dirpath)
  (cond
    (and exist? (= type :file)) (merge copy-action
                                       (try (copy-file path dst-dirpath options)
                                            {:status :success
                                             :method :file}
                                            (catch Exception e
                                              {:status :file-copy-fail
                                               :method :file
                                               :exception e})))
    (and exist? (= type :directory)) (merge copy-action
                                            (try (copy-dir path target-path options)
                                                 {:status :success
                                                  :method :directory}
                                                 (catch Exception e
                                                   {:status :dir-copy-fail
                                                    :method :directory
                                                    :exception e})))
    :else (assoc copy-action :status :skipped)))
