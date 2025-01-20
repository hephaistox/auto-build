(ns auto-build.os.edn-utils
  "Read an edn file."
  (:require [auto-build.code.formatter :as build-formatter]
            [auto-build.os.edn-utils.impl.reader]
            [auto-build.os.file :as build-file]))

(defn read-edn
  "Read `edn-filename` and returns a map with:
  * `:filepath` as given as a parameter
  * `:afilepath` file with absolute path
  * `:status` is `:success` or `:fail`
  * `:raw-content` if file can be read.
  * `:exception` if something wrong happened.
  * `:edn` if the text to edn translation is successful.

  That functions print on the cli:
  * nothing if successful or if printers are nil
  * an error message and the message of the exception if the file can't be read."
  [printers edn-filename]
  (auto-build.os.edn-utils.impl.reader/read-edn printers edn-filename))

(defn write-edn
  "Spit the `content` in the edn file called `edn-filename`.

  Params:
  * `edn-filepath` Filepath
  * `content` What is spitted
  Return nil if successful else map with :exception"
  [edn-filepath printers content]
  (let [filedesc (build-file/write-file edn-filepath printers content)
        {:keys [status]} filedesc]
    (merge {:edn-filepath edn-filepath, :status status, :filedesc filedesc}
           (when (= :success status)
             (let [formatting-res
                     (build-formatter/format-file printers "." edn-filepath)
                   {formatting-status :status} formatting-res]
               {:status formatting-status,
                :formatting formatting-res,
                :filedesc filedesc})))))
