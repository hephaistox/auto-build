(ns auto-build.tasks.clj-test
  (:require
   [auto-build.code.clj-compile :refer [compile-alias]]
   [auto-build.os.cli-opts      :as build-cli-opts]
   [auto-build.os.exit-codes    :as build-exit-codes]
   [auto-build.os.filename      :refer [absolutize]]
   [auto-build.project.deps     :as build-deps]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(defn cli-opts
  [deps-test-alias aliases-in-deps-edn]
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args
      (build-cli-opts/parse-argument-list deps-test-alias aliases-in-deps-edn)))

; ********************************************************************************
; *** Private
; ********************************************************************************

(defn- aliases-in-deps-edn
  [{:keys [errorln uri-str]
    :as printers}
   app-dir]
  (let [{:keys [status edn dir]} (build-deps/read printers app-dir)]
    (if-not (= :success status)
      (do (errorln "`deps.edn` file is invalid in dir"
                   (-> dir
                       absolutize
                       uri-str))
          nil)
      (->> edn
           :aliases
           keys
           (mapv name)))))

; ********************************************************************************
; *** Task code
; ********************************************************************************

(defn clj-test*
  "Run tests"
  [{:keys [subtitle title-valid title-error]
    :as printers}
   {:keys [valid-args]
    :as _cli-opts}
   app-dir
   test-runner-alias
   verbose]
  (let [exit-codes (mapv #(compile-alias printers verbose app-dir test-runner-alias %) valid-args)]
    (subtitle "clj-test of" valid-args)
    (if (every? #(= :success (:status %)) exit-codes)
      (do (title-valid "Tests passed") build-exit-codes/ok)
      (do (title-error "Tests have failed") build-exit-codes/invalid-state))))

(defn clj-test
  "Run tests. Return an exit code"
  [{:keys [title exceptionln errorln]
    :as printers}
   app-dir
   test-runner-alias
   current-task
   test-definitions]
  (title "clj-test")
  (try (if-let [aliases-in-deps-edn (aliases-in-deps-edn printers app-dir)]
         (let [cli-opts (cli-opts test-definitions aliases-in-deps-edn)
               verbose (get-in cli-opts [:options :verbose])
               exit-code (build-cli-opts/enter-args-in-a-list cli-opts
                                                              current-task
                                                              "TEST-ALIASES"
                                                              test-definitions)]
           (if exit-code exit-code (clj-test* printers cli-opts app-dir test-runner-alias verbose)))
         build-exit-codes/invalid-state)
       (catch Exception e
         (errorln "Unexpected error:")
         (exceptionln e)
         build-exit-codes/unexpected-exception)))
