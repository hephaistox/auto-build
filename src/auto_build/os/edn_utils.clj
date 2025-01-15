(ns auto-build.os.edn-utils
  "Read an edn file."
  (:require [auto-build.code.formatter :as build-formatter]
            [auto-build.os.file :as build-file]
            [auto-build.os.filename :as build-filename]
            [clojure.edn :as edn]))

(defn str->edn
  "Turns `raw-content` string into an edn"
  [raw-content]
  (edn/read-string raw-content))

(defn read-edn
  "Read `edn-filename` and returns a map with:
  * `:filepath`
  * `:afilepath`
  * `:raw-content` if file can be read.
  * `:invalid?` is boolean
  * `:exception` if something wrong happened.
  * `:edn` if the translation."
  ([{:keys [errorln uri-str], :as _printers} edn-filename]
   (let [res (build-file/read-file edn-filename)
         {:keys [raw-content invalid?]} res]
     (if (or (nil? raw-content) invalid?)
       res
       (try (assoc res :edn (str->edn raw-content))
            (catch Exception e
              (when (and (fn? errorln) (fn? uri-str))
                (errorln (str "File"
                              (-> edn-filename
                                  uri-str
                                  build-filename/absolutize)
                              "not found")))
              (assoc res
                :exception e
                :status :edn-failed))))))
  ([edn-filename] (read-edn nil edn-filename)))

(defn write-edn
  "Spit the `content` in the edn file called `edn-filename`.
  Params:
  * `edn-filename` Filename
  * `content` What is spitted
  Return nil if successful else map with :exception"
  [printers edn-filename content]
  (if (nil? edn-filename)
    {:exception "Impossible to save the file due to empty filename"}
    (let [file-desc (:status (build-file/write-file edn-filename content))]
      (when (= :success file-desc)
        (try (build-formatter/format-file printers edn-filename)
             file-desc
             (catch Exception e {:exception e}))))))
