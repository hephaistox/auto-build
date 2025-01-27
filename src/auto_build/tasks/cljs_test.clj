(ns auto-build.tasks.cljs-test
  (:require
   [auto-build.code.cljs-compile :refer [compile-alias]]
   [auto-build.os.cli-opts       :as build-cli-opts]
   [auto-build.os.exit-codes     :as build-exit-codes]
   [auto-build.os.filename       :refer [absolutize]]
   [auto-build.project.shadow    :as build-shadow]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(defn cli-opts
  [deps-test-alias aliases-defined]
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args
      (build-cli-opts/parse-argument-list deps-test-alias aliases-defined)))

; ********************************************************************************
; *** Private
; ********************************************************************************

(defn- aliases-in-shadow
  [{:keys [errorln uri-str]
    :as printers}
   app-dir
   target-type]
  (let [{:keys [status edn dir]} (build-shadow/read printers app-dir)]
    (if-not (= :success status)
      (do (errorln (uri-str build-shadow/shadow-cljs-edn-filename)
                   "file is invalid in dir"
                   (-> dir
                       absolutize
                       uri-str))
          nil)
      (-> (filter #(= target-type (:target (second %))) (:builds edn))
          (update-keys name)))))

; ********************************************************************************
; *** Task code
; ********************************************************************************

(defn cljs-test*
  [{:keys [subtitle title-valid title-error]
    :as printers}
   {:keys [valid-args]
    :as _cli-opts}
   app-dir
   verbose]
  (let [exit-codes (mapv #(compile-alias printers verbose app-dir %) valid-args)]
    (subtitle "cljs-test of" valid-args)
    (if (every? #(= :success (:status %)) exit-codes)
      (do (title-valid "Tests passed") build-exit-codes/ok)
      (do (title-error "Tests have failed") build-exit-codes/invalid-state))))

(defn node-test
  "Run tests. Return an exit code"
  [{:keys [exceptionln errorln title]
    :as printers}
   app-dir
   current-task
   test-definitions]
  (title "cljs-test")
  (try (if-let [aliases-in-deps-edn (aliases-in-shadow printers app-dir :node-test)]
         (let [cli-opts (cli-opts test-definitions (keys aliases-in-deps-edn))
               verbose (get-in cli-opts [:options :verbose])
               exit-code (build-cli-opts/enter-args-in-a-list cli-opts
                                                              current-task
                                                              "TEST-ALIASES"
                                                              test-definitions)]
           (if exit-code exit-code (cljs-test* printers cli-opts app-dir verbose)))
         build-exit-codes/invalid-state)
       (catch Exception e
         (errorln "Unexpected error:")
         (exceptionln e)
         build-exit-codes/unexpected-exception)))
