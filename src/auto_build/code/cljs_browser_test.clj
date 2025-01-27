(ns auto-build.code.cljs-browser-test
  (:require
   [auto-build.os.cmd :as build-cmd]))

(defn compile-alias
  [{:keys [normalln errorln subtitle uri-str]
    :as _printers}
   verbose
   app-dir
   alias]
  (subtitle "Tests in browser" alias "with clojurescript compiler")
  (let [res (build-cmd/print-verbosely verbose
                                       ["npx" "shadow-cljs" "watch" alias]
                                       app-dir
                                       normalln
                                       errorln
                                       10
                                       1000
                                       1000)
        {:keys [status]} res]
    (when-not (= status :success) (errorln "Error during execution of" (uri-str alias)))
    res))
