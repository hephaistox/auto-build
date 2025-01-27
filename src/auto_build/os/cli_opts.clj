(ns auto-build.os.cli-opts
  "Parse cli options.

  Proxy to [tools.cli](https://github.com/clojure/tools.cli)"
  (:require
   [auto-build.echo.base     :as build-base]
   [auto-build.os.exit-codes :as build-exit-codes]
   [clojure.set              :as set]
   [clojure.string           :as str]
   [clojure.tools.cli        :as tools-cli]))

;; ********************************************************************************
;; Private
;; ********************************************************************************

(def ^:private normalln build-base/normalln)
(def ^:private errorln build-base/errorln)

(defn- print-usage-msg
  "Returns the string for the summary of the task."
  [{:keys [summary]
    :as _parsed-cli-opts}
   current-task]
  (normalln "Usage: bb" (:name current-task) "[options]")
  (normalln)
  (normalln "Options:")
  (normalln summary))

(defn- print-usage-msg-with-args
  "Returns the string for the summary of the task."
  [{:keys [summary]
    :as _parsed-cli-opts}
   current-task
   argument-name
   argument-desc]
  (normalln "Usage: bb" (:name current-task) "[options]" argument-name)
  (normalln)
  (normalln argument-name "should be" argument-desc)
  (normalln "Options:")
  (normalln summary))

(defn- print-error-message
  [cli-opts current-task]
  (when-let [errors (:errors cli-opts)]
    (errorln (str/join \newline errors))
    (print-usage-msg cli-opts current-task)
    build-exit-codes/command-not-found))

(defn- print-verbose
  [cli-opts]
  (when (and (number? (get-in cli-opts [:options :verbose]))
             (<= 2 (get-in cli-opts [:options :verbose])))
    (normalln "Options are:")
    (normalln (pr-str cli-opts))
    nil))

(defn print-help-message
  [cli-opts current-task]
  (print-usage-msg cli-opts current-task)
  build-exit-codes/ok)

(defn- print-help
  [cli-opts current-task]
  (when (get-in cli-opts [:options :help]) (print-help-message cli-opts current-task)))

(defn- print-help-with-args
  [cli-opts current-task argument-name argument-desc]
  (when (get-in cli-opts [:options :help])
    (print-usage-msg-with-args cli-opts current-task argument-name argument-desc)
    build-exit-codes/ok))

(defn- print-no-args-required
  [cli-opts]
  (when-not (empty? (:arguments cli-opts))
    (errorln "No arguments are required: " (:arguments cli-opts))
    build-exit-codes/misuse))

;; ********************************************************************************
;; Public
;; ********************************************************************************

;; Definitions
(def help-options [["-h" "--help" "Print usage."]])

(def verbose-options
  [["-v" "--verbose" "Verbose" :update-fn (fn [val] (if (nil? val) 1 (inc val)))]])

(defn str-to-kw
  "Turn a string starting with : and turn it into an actual keyword. Returns `nil` otherwise"
  [s]
  (when (= \: (first s))
    (->> s
         rest
         (apply str)
         keyword)))

;; Parsing
(defn parse-cli-args
  "Parse `cli-args` (defaulted to actual cli arguments.) with `cli-options.`.

  Returns a map with `[options arguments errors summary]` fields."
  ([cli-options] (tools-cli/parse-opts *command-line-args* cli-options))
  ([cli-args cli-options] (tools-cli/parse-opts cli-args cli-options)))

(defn- replace-all
  [{:keys [arguments]
    :as cli-opts}
   arg-list]
  (cond-> cli-opts
    (contains? (set arguments) "all") (assoc :arguments arg-list)))

(defn parse-argument-list
  [parsed-cli-opts arg-list defined-args]
  (let [parsed-cli-opts (replace-all parsed-cli-opts arg-list)
        defined-args (set defined-args)
        arg-list (set arg-list)
        arguments (set (:arguments parsed-cli-opts))
        valid-args (set/intersection arguments arg-list defined-args)]
    (assoc parsed-cli-opts
           :defined-args defined-args
           :arg-list arg-list
           :valid-args valid-args
           :not-defined-arg-list (set/difference arg-list defined-args)
           :not-arg-list (set/difference arguments arg-list))))

;; ********************************************************************************
;; entering functions
;; ********************************************************************************

(defn enter
  "Enter task execution. No argument is required

  Returns `nil` if ok or an exit code if an error occured."
  [cli-opts current-task]
  (or (print-verbose cli-opts)
      (print-help cli-opts current-task)
      (print-error-message cli-opts current-task)
      (print-no-args-required cli-opts)))

(defn- print-arg-in-list
  [cli-opts current-task argument-name argument-desc]
  (let [{:keys [valid-args not-arg-list not-defined-arg-list]} cli-opts]
    (when (empty? valid-args) (errorln "A valid argument is mandatory"))
    (when-not (empty? not-defined-arg-list)
      (normalln "Warning: these arguments are skipped as they are not defined: "
                (str/join ", " not-defined-arg-list)))
    (when-not (empty? not-arg-list)
      (normalln "Warning: these arguments are skipped as they are not part of the list: "
                (str/join ", " not-arg-list)))
    (when (empty? valid-args)
      (print-usage-msg-with-args cli-opts current-task argument-name argument-desc)
      build-exit-codes/invalid-argument)))

(defn enter-args-in-a-list
  "As enter, but with arguments that should be in  `arg-list`.

  `argument-name` and `argument-desc` are used to display erros.

  Returns `nil` if ok or an exit code if an error occured."
  [cli-opts current-task argument-name arg-list]
  (let [argument-desc (apply str (concat ["["] (str/join "|" arg-list) ["]"]))]
    (or (print-verbose cli-opts)
        (print-help-with-args cli-opts current-task argument-name argument-desc)
        (print-error-message cli-opts current-task)
        (print-arg-in-list cli-opts current-task argument-name argument-desc))))

(comment
 ;; For an example:
 ;; (def cli-options
 ;;   [;; First three strings describe a short-option, long-option with
 ;;   optional
 ;;    ;; example argument description, and a description. All three are
 ;;    optional ;; and positional.
 ;;    ["-p"
 ;;     "--port PORT" "Port number" :default 80 :parse-fn
 ;;     #(Integer/parseInt %)
 ;;     :validate
 ;;     [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
 ;;    ["-H"
 ;;     "--hostname HOST"
 ;;     "Remote host"
 ;;     :default
 ;;     (InetAddress/getByName "localhost")
 ;;     ;; Specify a string to output in the default column in the options
 ;;     summary ;; if the default value's string representation is very
 ;;     ugly :default-desc "localhost" :parse-fn
 ;;     #(InetAddress/getByName %)]
 ;;    ;; If no required argument description is given, the option is
 ;;    assumed to ;; be a boolean option defaulting to nil
 ;;    [nil "--detach" "Detach from controlling process"]
 ;;    ["-v"
 ;;     nil "Verbosity level; may be specified multiple times to increase
 ;;     value"
 ;;      no long-option is specified, an option :id must be given
 ;;     :id :verbosity :default 0 ;; Use :update-fn to create
 ;;     non-idempotent options (:default is
 ;;     applied first)
 ;;     :update-fn
 ;;     inc]
 ;;    ["-f"
 ;;     "--file NAME" "File names to read" :multi true ; use :update-fn to
 ;;     combine multiple instance of -f/--file :default
 ;;     []
 ;;     ;; with :multi true, the :update-fn is passed both the existing
 ;;     parsed ;; value(s) and the new parsed value from each option
 ;;     :update-fn
 ;;     conj]
 ;;    ;; A boolean option that can explicitly be set to false
 ;;    ["-d" "--[no-]daemon" "Daemonize the process" :default true]
 ;;    ["-h" "--help"]])
 ;; (def cli-options
 ;;   ;; An option with a required argument
 ;;   [["-p"
 ;;     "--port PORT" "Port number" :default 80 :parse-fn
 ;;     #(Integer/parseInt %)
 ;;     :validate
 ;;     [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
 ;;    ;; A non-idempotent option (:default is applied first)
 ;;    ["-v" nil "Verbosity level" :id :verbosity :default 0 :update-fn
 ;;    inc] ; Prior to 0.4.1, you would have to use:
 ;;    ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
 ;;    ;; A boolean option defaulting to nil
 ;;    ["-h" "--help"]])
)
