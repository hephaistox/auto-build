(ns auto-build.tasks.repl
  (:require
   [auto-build.os.cli-opts   :as build-cli-opts]
   [auto-build.os.exit-codes :refer [ok]]
   [babashka.process         :as p]
   [clojure.string           :as str]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def detailed-message
  ["Merge the content below to `.clojure/deps.edn` to create a repl adapter to your IDE"
   " {:aliases"
   "  {;; Launch a CIDER-friendly nREPL"
   "   :repl"
   "     {:extra-deps {cider/cider-nrepl{:mvn/version \"0.55.7\"},"
   "                   nrepl/nrepl {:mvn/version \"1.1.1\"},"
   "                   refactor-nrepl/refactor-nrepl #:mvn{:version \"3.10.0\"}},"
   "      :main-opts"
   "        [\"-m\" \"nrepl.cmdline\" \"--middleware\""
   "         \"[cider.nrepl/cider-middleware,refactor-nrepl.middleware/wrap-refactor]\"]}}}"])

(defn print-detailed-message [] (doseq [x detailed-message] (println x)))

(def cli-opts
  (-> [["-e" "--example" "Deps.edn example."]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn repl*
  [app-dir repl-aliases repl-port]
  (let [repl-alias ":repl"
        cmd ["clojure" (str "-M" (str/join "" repl-aliases) repl-alias) "--port" repl-port]]
    (println "Execute" cmd)
    (-> (apply p/shell
               {:continue true
                :dir app-dir}
               cmd)
        :exit)))

(defn repl
  [{:keys [title]
    :as _printers}
   app-dir
   current-task
   repl-aliases
   repl-port]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (if (get-in cli-opts [:options :example])
      (do (print-detailed-message) ok)
      (let [title-msg (str "Start clj repl - alias " repl-aliases)]
        (title title-msg)
        (repl* app-dir repl-aliases repl-port)))))
