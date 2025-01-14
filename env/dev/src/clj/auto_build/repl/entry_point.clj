(ns auto-build.repl.entry-point
  "Entry point for repl"
  (:require [auto-build.os.exit-codes :as exit-codes]
            [auto-build.os.colorized-text :refer [bg-red style-reset-all]]
            [auto-build.repl.port-number :refer [port-number]]
            [cider.nrepl.middleware :as mw]
            [clojure.string :as str]
            [nrepl.server :refer [default-handler start-server]]
            [refactor-nrepl.middleware]))

(def custom-nrepl-handler
  "Custom nrepl handler."
  (apply default-handler
    (conj cider.nrepl.middleware/cider-middleware
          'refactor-nrepl.middleware/wrap-refactor)))

(defn -main
  "Entry point"
  [& _args]
  (let [port (port-number)]
    (println "Start repl on port:" port)
    (try
      (start-server :port port :handler custom-nrepl-handler)
      (spit ".nrepl-port" port)
      (catch Exception e
        (when (str/includes? (ex-message e) "Address already in use")
          (println bg-red "Stopped as adress is already in use" style-reset-all)
          (System/exit exit-codes/misuse))))))
