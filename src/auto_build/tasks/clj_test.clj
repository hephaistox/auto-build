(ns auto-build.tasks.clj-test
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.os.cmd :as build-cmd]
            [auto-build.project.deps :as build-deps]
            [auto-build.os.exit-codes :as build-exit-codes]
            [auto-build.os.filename :refer [absolutize]]))

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

(defn- test-cmd
  [test-runner-alias test-definitions]
  (->> test-definitions
       (mapv (fn [alias]
               {:cmd ["clojure" (str "-M:" test-runner-alias ":" alias)],
                :alias alias}))))

(defn- aliases-in-deps-edn
  [{:keys [errorln uri-str], :as printers} app-dir]
  (let [{:keys [status edn dir]} (build-deps/read printers app-dir)]
    (if-not (= :success status)
      (do (errorln "`deps.edn` file is invalid in dir "
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

(defn clj-test
  "Run tests"
  [{:keys [title title-valid title-error subtitle subtitle-error normalln
           errorln],
    :as printers} app-dir test-runner-alias current-task test-definitions]
  (title "clj-test")
  (try
    (if-let [aliases-in-deps-edn (aliases-in-deps-edn printers app-dir)]
      (let [cli-opts (cli-opts test-definitions aliases-in-deps-edn)
            verbose (get-in cli-opts [:options :verbose])
            exit-code (build-cli-opts/enter-args-in-a-list cli-opts
                                                           current-task
                                                           "TEST-ALIASES"
                                                           test-definitions)]
        (if exit-code
          exit-code
          (let [{:keys [valid-args]} cli-opts
                exit-codes
                  (->> valid-args
                       (test-cmd test-runner-alias)
                       (keep
                         (fn [{:keys [cmd alias]}]
                           (subtitle "Tests" alias)
                           (let [{:keys [status]}
                                   (if verbose
                                     (build-cmd/printing cmd
                                                         app-dir
                                                         normalln
                                                         errorln
                                                         10)
                                     (build-cmd/print-on-error cmd
                                                               app-dir
                                                               normalln
                                                               errorln
                                                               10
                                                               100
                                                               100))]
                             (when-not (= :success status)
                               (subtitle-error "Tests" alias "have failed"))
                             status))))]
            (title "Tested environments:" valid-args)
            (if (every? #(= :success %) exit-codes)
              (do (title-valid "Tests passed") build-exit-codes/ok)
              (do (title-error "Tests have failed")
                  build-exit-codes/invalid-state))
            build-exit-codes/ok)))
      build-exit-codes/invalid-state)
    (catch Exception e (println "Unexpected error:") (println (pr-str e)))))
