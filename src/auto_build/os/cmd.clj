(ns auto-build.os.cmd
  "Execute an external command, called **cmd**.

  `cmd` are made of a vector of strings like `[\"ls\" \"-la\"]`

  There are many flavors to execute this command, the generic one made of `create-process`, `wait-for`, ... and the helpers to simplify some typical use cases.

  All functions deal with a **process**, a map describing the command execution and then enriched as the process is going on."
  (:refer-clojure :exclude [delay])
  (:require [auto-build.data.fixed-size-queue :as build-fix-size-queue]
            [auto-build.os.filename :as build-filename]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn parameterize "Turns `cmd` to a cmd parameter" [cmd] (str "'\"" cmd "\"'"))

(defn when-success?
  "Returns `printers` if `res` is successful, `nil` otherwise"
  ([res printers message] (when-success? res printers message nil))
  ([{:keys [status], :as _res} {:keys [subtitle errorln], :as printers} message
    error-message]
   (if (= :success status)
     (do (when message (subtitle message)) printers)
     (do (when error-message (errorln error-message)) nil))))

(defn final-cmd
  "Returns the status and print the message if status is successful"
  [{:keys [status], :as _res} {:keys [subtitle title], :as _printers} message
   end-message]
  (when message (subtitle message))
  (when end-message (title end-message))
  status)

(defn- watch-proc-stream
  "Watch the `stream-kw` process in the `bb-proc` (babashka process) and listen to each new line in the stream:
  * stores the line in `a-stream`, an atom containing a fixed size queue of strings
  * prints the line with `on-new-line`

  To save resources, if the stream is empty a pause of `delay` milliseconds is waited before checking again

  `a-stream` and `on-new-line` could be `nil` and will skip their behavior"
  [a-stream bb-proc stream-kw on-new-line delay]
  (with-open [rdr (io/reader (get bb-proc stream-kw))]
    (let [proc-in-bb-proc (:proc bb-proc)
          delay (if (and (number? delay) (pos? delay)) delay 10)]
      (binding [*in* rdr]
        (loop []
          (let [line (read-line)]
            (when line
              (when on-new-line (on-new-line line))
              (when a-stream
                (swap! a-stream build-fix-size-queue/enqueue line)))
            (let [remaining-lines? (.ready rdr)
                  proc-alive? (.isAlive proc-in-bb-proc)]
              (when (and (empty? line) (not remaining-lines?))
                (Thread/sleep delay))
              (when (or remaining-lines? proc-alive?) (recur)))))))))

;; ********************************************************************************
;; Detailed API
;; ********************************************************************************

(defn to-str
  "Turns `cmd` into an executable command"
  [cmd]
  (str "`" (str/join " " cmd) "`"))

(defn create-process
  "Starts the execution of the command `cmd` in directory `dir` and store command outputs in atoms respectively named in `:out-stream` and `:err-stream`.
  To save resources, if the stream is empty a pause of `delay` milliseconds is waited before checking again
  Only `max-out-lines` and `max-err-lines` are stored in the atoms, if `nil` no value is stored at all.

  It's a non blocking function, next step could be `still-running?`, `wait-for` or `kill`.

  Returns a `process` with:

   * `:cmd` the command, e.g. [\"ls\" \"-la\"]
   * `:cmd-str` the command as a string e.g. `ls -la`
   * `:dir` the directory as provided by the user, e.g. \"\"
   * `:status` is a value among `:wip`, `:didnt-start`, `:success`, `:fail`
   * `:adir` the expanded directory (useful for debug), e.g. \"/User/johndoe/hephaistox\"

   * If the command starts successfully, it returns also
      * `:status = :wip`
      * `:bb-proc` the babashka process, normally for internal use only
      * `out-stream` a fixed size queue of lines to print on out stream
      * `err-stream` a fixed size queue of lines to print on err stream
   * If not - most probably if directory or command does not exist - returns a `process` with
      * `status=:cmd-doesnt-exist`
      * and prints an error message

  `cant-start` is executed if the command execution can't start.
  `on-end` will be executed once, at the end of an execution"
  [cmd dir on-out on-err on-end delay cant-start max-out-lines max-err-lines]
  (if (empty? cmd)
    {:status :empty-cmd}
    (let [adir (-> (if (str/blank? dir) "." dir)
                   build-filename/absolutize)
          out-lines (when (number? max-out-lines)
                      (atom (build-fix-size-queue/init max-out-lines)))
          err-lines (when (number? max-err-lines)
                      (atom (build-fix-size-queue/init max-err-lines)))
          cmd-str (str/join " " cmd)]
      (merge
        {:cmd cmd, :cmd-str cmd-str, :dir dir, :status :wip, :adir adir}
        ;; Create a process that will destroy itself, will not create
        ;; any exception, and not print anything
        (try (let [bb-proc (p/process {:shutdown p/destroy-tree,
                                       :continue true,
                                       :out nil,
                                       :err nil,
                                       :dir adir}
                                      cmd-str)]
               (future (watch-proc-stream out-lines bb-proc :out on-out delay)
                       (when on-end (on-end)))
               (future (watch-proc-stream err-lines bb-proc :err on-err delay))
               {:bb-proc bb-proc, :out-stream out-lines, :err-stream err-lines})
             (catch Exception exception
               (when cant-start (cant-start cmd))
               {:exception exception, :status :didnt-start}))))))

(defn still-running?
  "Returns true if the `process` is still living?"
  [process]
  (let [bb-proc (:bb-proc process)] (if bb-proc (p/alive? bb-proc) false)))

(defn wait-for
  "Block the current thread execution until the end of `process`.

  This function is noop if the status is different from :wip

  Otherwise, the process is executed, if it is failing:
     * functions `out-print-ln` and `on-err` are called with `(on-out line)`. That functions could be `nil`.

  Returns the `process` with `out-stream` and `err-stream` turned into vector of strings."
  [{:keys [bb-proc cmd out-stream err-stream status], :as process} on-out
   on-err]
  (if (and (= :wip status) bb-proc)
    (let [process (update process :bb-proc deref)
          exit (get-in process [:bb-proc :exit])
          status (if (= 0 exit) :success :fail)]
      (when (and on-err (not= :success status))
        (on-err (str "Error during execution of " (to-str cmd))))
      ;; It's important to print message first, as stream manipulation
      ;; could be long, and message order messed up
      (let [out-stream (-> out-stream
                           deref
                           build-fix-size-queue/content)
            err-stream (-> err-stream
                           deref
                           build-fix-size-queue/content)]
        (when (and on-out (not= :success status))
          (run! on-out out-stream)
          (when-not (empty? err-stream)
            (on-out "Error stream:")
            (run! on-out err-stream)))
        (cond-> (assoc process :status status)
          out-stream (assoc :out-stream out-stream)
          err-stream (assoc :err-stream err-stream))))
    process))

(defn kill
  "Kill `process` if it is still running

  Returns `process` with `killed`."
  [{:keys [bb-proc cmd-str], :as process} on-out]
  (when (and still-running? process bb-proc)
    (p/destroy-tree bb-proc)
    (when (fn? on-out) (on-out "process `" cmd-str "is killed"))
    (assoc process
      :killed? true
      :success? false)))

(defn exec-cmd
  "Returns the string of the execution of a command `cmd`"
  [cmd]
  (str "execute command `" (to-str cmd) "`"))

;; ********************************************************************************
;; High level API
;; ********************************************************************************
(defn muted
  "When only exit code is useful.

  Print nothing, even errors, is blocking."
  [cmd dir]
  (-> (create-process cmd dir nil nil nil 0 nil 0 0)
      (wait-for nil nil)))

(defn muted-non-blocking
  "When only side effects are important, no feedback on terminal. Most probably elsewhere"
  [cmd dir]
  (create-process cmd dir nil nil nil 0 nil 0 0))

(defn as-string
  "Returns only data in process, as strings"
  ([cmd dir] (as-string cmd dir 1000 1000))
  ([cmd dir max-out-lines max-err-lines]
   (-> (create-process cmd dir nil nil nil 1 nil max-out-lines max-err-lines)
       (wait-for nil nil))))

(defn printing-non-blocking
  "When a command should be printed on the terminal, like a REPL, a compilation, ....

  This command is non blocking"
  [cmd dir on-out on-err on-end delay]
  (create-process cmd
                  dir
                  on-out
                  on-err
                  on-end
                  delay
                  #(on-err "Cant' start" %)
                  0
                  0))

(defn printing
  "Print the whole command execution on the terminal. Is blocking until the end."
  [cmd dir on-out on-err delay]
  (when on-out (on-out "Execute" cmd))
  (-> (create-process cmd
                      dir
                      on-out
                      on-err
                      nil
                      delay
                      #(on-err "Cant' start" %)
                      0
                      0)
      (wait-for nil nil)))

(defn print-on-error
  "Does not print on the terminal, except if an error occur."
  [cmd dir on-out on-err delay max-out-lines max-err-lines]
  (-> (create-process cmd
                      dir
                      nil
                      nil
                      nil
                      delay
                      #(on-err "Cant' start" %)
                      max-out-lines
                      max-err-lines)
      (wait-for on-out on-err)))

(defn print-verbosely
  [verbose cmd dir on-out on-err delay max-out-lines max-err-lines]
  (if verbose
    (printing cmd dir on-out on-err delay)
    (print-on-error cmd dir on-out on-err delay max-out-lines max-err-lines)))
