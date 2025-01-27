(ns auto-build.repl.entry-point
  "Entry point for repl"
  (:require
   [auto-build.echo          :refer [level1-header]]
   [auto-build.os.exit-codes :as exit-codes]
   [cider.nrepl.middleware   :as mw]
   [clojure.string           :as str]
   [nrepl.server             :refer [default-handler start-server]]
   [refactor-nrepl.middleware]))

(def custom-nrepl-handler
  "Custom nrepl handler."
  (apply default-handler
         (conj cider.nrepl.middleware/cider-middleware 'refactor-nrepl.middleware/wrap-refactor)))

(defn start
  "Entry point"
  [{:keys [port]} & _rargs]
  (let [{:keys [normalln errorln exceptionln]
         :as _printers}
        level1-header]
    (normalln "Start repl on port:" port)
    (try (spit ".nrepl-port" port)
         (start-server :port port :handler custom-nrepl-handler)
         (let [blocking-promise (promise)] @blocking-promise)
         (catch Exception e
           (if (str/includes? (ex-message e) "Address already in use")
             (do (errorln "Stopped as adress is already in use") (System/exit exit-codes/misuse))
             (do (errorln "Unknown error") (exceptionln e)))))))
