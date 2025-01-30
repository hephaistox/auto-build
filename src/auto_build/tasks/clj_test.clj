(ns auto-build.tasks.clj-test
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :as    build-cmd
                             :refer [execute-if-success]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [auto-build.os.filename   :refer [absolutize]]
   [auto-build.project.deps  :as build-deps]))

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
  [{:keys [uri-str]
    :as printers}
   {:keys [valid-args]
    :as _cli-opts}
   app-dir
   test-runner-alias
   verbose]
  (-> (reduce (fn [res alias]
                (execute-if-success res
                                    printers
                                    app-dir
                                    verbose
                                    ["clojure" (str "-M:" test-runner-alias ":" alias)]
                                    (str (uri-str alias) " alias is tested")
                                    (str (uri-str alias) "alias test has failed")
                                    (keyword (str "test-" alias))
                                    nil))
              {:status :success}
              valid-args)))

(defn clj-test
  "Run tests. Return an exit code"
  [{:keys [title]
    :as printers}
   app-dir
   test-runner-alias
   current-task
   test-definitions]
  (if-let [aliases-in-deps-edn (aliases-in-deps-edn printers app-dir)]
    (let [cli-opts (cli-opts test-definitions aliases-in-deps-edn)
          verbose (get-in cli-opts [:options :verbose])]
      (if-let [exit-code (build-cli-opts/enter-args-in-a-list cli-opts
                                                              current-task
                                                              "TEST-ALIASES"
                                                              test-definitions)]
        exit-code
        (let [title-msg "clj-test"]
          (title title-msg)
          (-> (clj-test* printers cli-opts app-dir test-runner-alias verbose)
              (build-cmd/status-to-exit-code printers title-msg)))))
    build-exit-codes/invalid-state))
