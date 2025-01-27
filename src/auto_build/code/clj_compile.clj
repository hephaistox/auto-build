(ns auto-build.code.clj-compile
  (:require
   [auto-build.os.cmd :as build-cmd]))

(defn compile-alias
  [{:keys [normalln errorln subtitle uri-str]
    :as _printers}
   verbose
   app-dir
   test-runner-alias
   alias]
  (subtitle "Tests" alias "with clojure compiler")
  (let [res (build-cmd/print-verbosely verbose
                                       ["clojure" (str "-M:" test-runner-alias ":" alias)]
                                       app-dir
                                       normalln
                                       errorln
                                       10
                                       1000
                                       1000)
        {:keys [status]} res]
    (when-not (= status :success) (errorln "Error during execution of" (uri-str alias)))
    res))
