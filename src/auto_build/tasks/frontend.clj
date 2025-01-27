(ns auto-build.tasks.frontend
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as build-cmd]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn- execute-cmd
  [{:keys [normalln errorln]
    :as _printers}
   app-dir
   verbose
   cmd
   concept-kw
   error-msg
   stream-to-res-fn]
  (when verbose (normalln "Execute" cmd))
  (let [res (build-cmd/as-string cmd app-dir 100 100)
        {:keys [status out-stream err-stream]} res]
    (merge {:status status
            concept-kw res}
           (if (= status :success)
             (stream-to-res-fn status out-stream)
             (do (errorln error-msg)
                 (when-not (empty? out-stream) (apply normalln out-stream))
                 (when-not (empty? err-stream) (apply normalln err-stream))
                 {:status :cmd-failed})))))

(defn- build-cljs
  [printers app-dir]
  (execute-cmd printers
               app-dir
               verbose
               ["npm" "install"]
               :a
               "Error during npm installation"
               (fn [{}] {})))

(defn dev-mode
  [printers app-dir]
  (some-> printers
          (build-cljs app-dir)))

(comment
  (do (println "Build production uberjar")
      (shell "npm install ")
      (shell "npx shadow-cljs release app")
      (shell "clj -T:uberjar")
      (shell "git init -b master" {:dir target-dir})
      (shell (str "git remote add clever " (System/getenv "SASU_CAUMOND_PROD_REPO"))
             {:dir target-dir})
      (shell "git add ." {:dir target-dir})
      (shell "git commit -m \"auto\"" {:dir target-dir}))
  ;
)
