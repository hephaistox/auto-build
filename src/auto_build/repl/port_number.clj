(ns auto-build.repl.port-number
  "Read the port number.

  This namespace is on purpose independant from the rest of the project.

  * See [ADR-3](https://github.com/hephaistox/auto-build/wiki/adr#adr)."
  (:require
   [clojure.edn :as edn]))

(defn port-number
  [{:keys [errorln normalln]
    :as _printers}]
  (let [default-port 8000]
    (if-let [repl-port-file "deps.edn"]
      (try (let [port (-> repl-port-file
                          slurp
                          edn/read-string
                          :aliases
                          :repl
                          :exec-args
                          :port)]
             (cond
               (nil? port) (errorln "`:port` key should be defined, defaulted to" default-port)
               (not (integer? port)) (errorln "`:port` key should be an Integer, defaulted to"
                                              default-port)
               :else port)
             (if (integer? port) port default-port))
           (catch Exception e
             (errorln "`repl_port.edn` is not a valid edn, default to" default-port)
             (normalln (ex-message e))
             default-port))
      (do (errorln "Impossible to find `repl_port.edn`, defaulted to" default-port) default-port))))
