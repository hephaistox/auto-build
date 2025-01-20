(ns auto-build.tasks.heph-info
  (:require [auto-build.os.cli-opts :as build-cli-opts]
            [auto-build.repl.port-number :refer [port-number]]
            [auto-build.os.exit-codes :as build-exit-codes]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options)
      build-cli-opts/parse-cli-args))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn heph-info
  "Print project infos"
  [{:keys [title], :as printers} current-task]
  (title "Heph info")
  (let [exit-code (build-cli-opts/enter cli-opts current-task)]
    (if exit-code
      exit-code
      (do (-> {:port (port-number printers)}
              pr-str
              println)
          build-exit-codes/ok))))
