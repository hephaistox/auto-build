(ns auto-build.tasks.cljs-node-test
  (:require
   [auto-build.os.cli-opts    :as build-cli-opts]
   [auto-build.os.cmd         :as    build-cmd
                              :refer [execute-if-success]]
   [auto-build.os.exit-codes  :as build-exit-codes]
   [auto-build.os.filename    :refer [absolutize]]
   [auto-build.project.shadow :as build-shadow]
   [clojure.string            :as str]))

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

(defn cljs-node-test*
  "Remove file from `filepaths` and directories from `dirs`"
  [{:keys [uri-str]
    :as printers}
   app-dir
   {:keys [valid-args]
    :as _cli-opts}
   _verbose]
  (let [aliases valid-args
        text-aliases (->> aliases
                          (mapv uri-str)
                          (str/join ", "))]
    (-> {:status :success}
        (execute-if-success printers
                            app-dir
                            true
                            ["npx" "shadow-cljs" "release" (str/join ":" aliases)]
                            ;;NOTE Compilation is enough as it `:autorun` parameter is true
                            (str "Build cljs release for aliases " text-aliases)
                            (str "Build cljs release for aliases " text-aliases " has failed")
                            :build-cljs-aliases))))


(defn cljs-node-test
  "Remove file from `filepaths` and directories from `dirs`"
  [{:keys [title]
    :as printers}
   app-dir
   current-task
   test-definitions]
  (if-let [aliases-in-deps-edn (aliases-in-shadow printers app-dir :node-test)]
    (let [cli-opts (cli-opts test-definitions (keys aliases-in-deps-edn))
          verbose (get-in cli-opts [:options :verbose])]
      (if-let [exit-code (build-cli-opts/enter-args-in-a-list cli-opts
                                                              current-task
                                                              "TEST-ALIASES"
                                                              test-definitions)]
        exit-code
        (let [title-msg "cljs-test of"]
          (title title-msg)
          (-> (cljs-node-test* printers app-dir cli-opts verbose)
              (build-cmd/status-to-exit-code printers title-msg)))))
    build-exit-codes/invalid-state))
