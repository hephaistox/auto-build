(ns auto-build.repl.cider-entry-point
  "Entry point for repl"
  (:require
   [auto-build.echo          :refer [level1-header]]
   [auto-build.os.exit-codes :as exit-codes]
   [clojure.string           :as str]
   [nrepl.server]))

(defn start
  "Entry point"
  [{:keys [port]} & _rargs]
  (let [{:keys [normalln errorln exceptionln]
         :as _printers}
        level1-header]
    (normalln "Start repl on port:" port)
    (try
      (try (spit ".nrepl-port" port)
           (nrepl.server/start-server
            :port port
            :handler (if (find-ns 'cider.nrepl.middleware)
                       #_{:clj-kondo/ignore [:unresolved-namespace]}
                       (apply nrepl.server/default-handler
                              (conj (resolve 'cider.nrepl.middleware/cider-middleware)
                                    'refactor-nrepl.middleware/wrap-refactor))
                       nrepl.server/default-handler))
           (let [blocking-promise (promise)] @blocking-promise)
           (catch Exception e
             (if (str/includes? (ex-message e) "Address already in use")
               (do (errorln "Stopped as adress is already in use") (System/exit exit-codes/misuse))
               (do (errorln "Unknown error") (exceptionln e)))))
      (catch Exception _
        (errorln "Add the following")
        (normalln
         "{:aliases {:cider-repl {:deps {cider/cider-nrepl #:mvn{:version \"0.52.0\"}
                                                    cider/piggieback {:mvn/version \"0.6.0\"}
                                                    refactor-nrepl/refactor-nrepl
                                                    #:mvn{:version \"3.10.0\"}}}}}")))))
