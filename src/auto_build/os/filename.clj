(ns auto-build.os.filename
  "Manipulate file names (is not influenced at all by your local configuration)."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def directory-separator
  "Symbol to separate directories.
  Is usually `/` on linux based OS And `\\` on windows based ones"
  fs/file-separator)

(defn remove-trailing-separator
  "If exists, remove the trailing separator in a path, remove unwanted spaces either"
  [path]
  (if (nil? path)
    ""
    (when [path (str/trim path)]
      (if (= (str directory-separator) (str (last path)))
        (->> (dec (count path))
             (subs path 0)
             remove-trailing-separator)
        path))))

(defn absolutize
  "Returns the absolute path of `relative-path` (file or dir)."
  [relative-path]
  (let [relative-path (if (nil? relative-path) "" relative-path)]
    (str (fs/absolutize relative-path))))

(defn match-extension?
  "Returns true if the `filename` match the at least one of the `extensions`."
  [filename & extensions]
  (boolean (when-not (str/blank? filename)
             (some (fn [extension] (str/ends-with? filename extension))
                   extensions))))

(defn change-extension
  "Turns `filename` extension into `new-extension`."
  [file-name new-extension]
  (str (fs/strip-ext file-name) new-extension))

(defn create-file-path
  "Creates a path for which each element of `dirs` is a subdirectory."
  [& dirs]
  (-> (if (some? dirs)
          (->> dirs
               (mapv str)
               (filter #(not (str/blank? %)))
               (mapv remove-trailing-separator)
               (interpose directory-separator)
               (apply str))
          "./")
      str))

(defn create-dir-path
  "Creates a path with the list of parameters.
  Removes the empty strings, add needed separators, including the trailing ones"
  [& dirs]
  (if (empty? dirs)
    "."
    (str (apply create-file-path dirs) directory-separator)))

(defn relativize
  "Turn the `path` into a relative directory starting from `root-dir`"
  [path root-dir]
  (let [path (-> path
                 remove-trailing-separator
                 absolutize)
        root-dir (-> root-dir
                     remove-trailing-separator
                     absolutize)]
    (when-not (str/blank? root-dir)
      (->> path
           (fs/relativize root-dir)
           str))))

(defn is-absolute?
  "Returns true if `path` is an absolute directory."
  [path]
  (= (str directory-separator) (str (first path))))

(defn extract-path
  "Extract the directory path to the `filename`."
  [filename]
  (when-not (str/blank? filename)
    (if (or (fs/directory? filename)
            (= directory-separator (str (last filename))))
      filename
      (let [filepath (->> filename
                          fs/components
                          butlast
                          (mapv str))]
        (cond (= [] filepath) ""
              (is-absolute? filename)
                (apply create-dir-path directory-separator filepath)
              :else (apply create-dir-path filepath))))))

(defn parent "Returns the parent of `path`." [path] (fs/parent path))

(defn filename
  "Returns the filename of a path."
  [full-path]
  (fs/file-name full-path))
