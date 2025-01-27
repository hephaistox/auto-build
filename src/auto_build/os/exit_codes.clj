(ns auto-build.os.exit-codes
  "Constants to have clear and consistent return codes for the application")

(def ok 0)

(def general-errors "1: Catchall for general errors" 1)

(def misuse "2: Misuse of shell built-ins (according to Bash documentation)" 2)

(def invalid-state "3: State is not consistent with the command" 3)

(def cannot-execute "126: Command invoked cannot execute" 126)

(def command-not-found "127: Command not found" 127)

(def invalid-argument "128: Invalid argument to exit" 128)

(defn fatal-error-signal "128+n: Fatal error signal \"n\"" [signal] (+ 128 signal))

(def unexpected-exception 129)

(def script-terminated "130 Script terminated by Control-C" 130)

(def exist-status-out-of-range "255\\ Exit status out of range" 255)

(def intentional "131 - Intentional error" 131)

(def rules-broken "132 - A rules is broken, execution is stopped" 132)

