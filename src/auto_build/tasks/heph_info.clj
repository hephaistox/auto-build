(ns auto-build.tasks.heph-info
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.cmd        :refer [as-string]]
   [auto-build.os.exit-codes :as build-exit-codes]
   [clojure.pprint]))

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
  [{:keys [title]} current-task dir]
  (title "Heph info")
  (let [exit-code (build-cli-opts/enter cli-opts current-task)]
    (if exit-code
      exit-code
      (do (-> {:versions {:java (-> ["java" "--version"]
                                    (as-string dir)
                                    :out-stream)
                          :clj-cli (-> ["clj" "--version"]
                                       (as-string dir)
                                       :out-stream)
                          :bb (-> ["bb" "--version"]
                                  (as-string dir)
                                  :out-stream)
                          :clj-kondo (-> ["clj-kondo" "--version"]
                                         (as-string dir)
                                         :out-stream)
                          :zprint (-> ["zprint" "--version"]
                                      (as-string dir)
                                      :err-stream)
                          :fd (-> ["fd" "--version"]
                                  (as-string dir)
                                  :out-stream)}}
              clojure.pprint/pprint)
          build-exit-codes/ok))))
