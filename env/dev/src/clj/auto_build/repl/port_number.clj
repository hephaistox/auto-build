(ns auto-build.repl.port-number
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn port-number
  []
  (-> "repl_port.edn"
      io/resource
      slurp
      edn/read-string
      :port))
