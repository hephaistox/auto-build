(ns auto-build.echo.actions
  "Echoing actions in terminal

  It is specially made for long living and possibly parrallel actions (like REPL and so on.)."
  (:require
   [auto-build.echo.base         :as    build-echo-base
                                 :refer [cmd-str]]
   [auto-build.os.colorized-text :as build-text]
   [clojure.string               :as str]))

;; ********************************************************************************
;; Private
;; ********************************************************************************
(defn- prefixs-to-str
  "Turns a `prefixs` collection into a `string`."
  [prefixs]
  (str (str/join "-" prefixs) ">"))

(defn- pure-printing
  [prefixs texts]
  (doseq [text texts] (println (str (prefixs-to-str prefixs) text)))
  (print build-text/font-default))

;; ********************************************************************************
;; Standardized echoing functions
;; ********************************************************************************
(defn normalln
  "Print as normal text the collection of `texts`, with the `prefixs` added.
  It is the default printing method."
  [prefixs & texts]
  (when-not (empty? texts) (pure-printing prefixs texts)))

(defn errorln
  "Print as an error the `texts`, with the `prefixs` added.

  It should be highlighted and rare (like one line red for each error and not its details)."
  [prefixs & texts]
  (when-not (empty? texts) (print build-text/font-red) (pure-printing prefixs texts)))

(defn exceptionln
  "Display exception `e`."
  [prefixs e]
  (errorln prefixs (ex-cause e))
  (normalln prefixs e))

(defn print-exec-cmd
  "Prints the execution of command string `cmd` with the `prefixs` added."
  [prefixs cmd]
  (normalln prefixs (cmd-str cmd)))

;; ********************************************************************************
;; Action specific echoing functions
;; ********************************************************************************
(defn action
  "Print an action with its `prefixs`, the text of the action is `texts`."
  [prefixs & texts]
  (print build-text/font-green)
  (pure-printing prefixs texts))

(def printers
  "Printers for actions"
  (merge build-echo-base/printers
         {:normalln normalln
          :errorln errorln
          :exceptionln exceptionln
          :print-exec-cmd print-exec-cmd
          :action action}))
