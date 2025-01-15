(ns auto-build.code.formatter
  "Format code to apply rules in `zprintrc`.

  Proxy to [zprint](https://github.com/kkinnear/zprint)"
  (:refer-clojure :exclude [format])
  (:require [auto-build.os.file :as build-file]
            [auto-build.os.cmd :as build-cmd]))

(def ^:private use-local-zprint-config-parameter
  "As described in the documentation below, by default `zprint` uses the local configuration.

  If this parameter is set locally, the project configuration will bee used.
  [zprint documentation](https://github.com/kkinnear/zprint/blob/main/doc/using/project.md#use-zprint-with-different-formatting-for-different-projects)"
  #":search-config\?\s*true")

(defn formatter-setup
  "Returns a map describing if zprint. "
  []
  (let [home-setup-file-desc (build-file/read-file (build-file/expand-home-str
                                                     "~/.zprintrc"))
        {:keys [raw-content invalid?], :as resp} home-setup-file-desc]
    (if invalid?
      resp
      (if (re-find use-local-zprint-config-parameter raw-content)
        {:message "zprint use properly project setup.", :status :ok}
        {:message
           "zprint local configuration is missing. Please add `:search-config? true` in your `~/.zprintc`",
         :status :ko}))))

(defn format-clj-cmd
  "Command formatting all clj files in the directory and subdirectories where it is executed."
  []
  ["fd" "-e" "clj" "-e" "cljc" "-e" "cljs" "-e" "edn" "-x" "zprint" "-w"])

(defn format-clj
  [{:keys [normalln errorln], :as _printers} app-dir verbose]
  (let [{:keys [message status]} (formatter-setup)
        format-clj-cmd (format-clj-cmd)]
    (if (= status :ko)
      (errorln message)
      (if verbose
        (build-cmd/printing format-clj-cmd app-dir normalln errorln nil 10)
        (build-cmd/print-on-error format-clj-cmd
                                  app-dir
                                  normalln
                                  errorln
                                  10
                                  100
                                  100)))))

(defn format-file-cmd
  "Command to format the `filename` with zprint."
  [filename]
  ["zprint" "-w" filename])

(defn format-file
  [{:keys [normalln errorln], :as _printers} filename]
  (let [{:keys [message status]} (formatter-setup)]
    (cond (= status :ko) (errorln message)
          (not (build-file/is-existing-file? filename))
            (-> (format-file-cmd filename)
                (build-cmd/print-on-error "." normalln errorln 10 100 100)))))
