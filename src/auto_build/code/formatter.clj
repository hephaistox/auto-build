(ns auto-build.code.formatter
  "Format code to apply rules in `zprintrc`.

  Proxy to [zprint](https://github.com/kkinnear/zprint)"
  (:refer-clojure :exclude [format])
  (:require
   [auto-build.os.cmd                   :as build-cmd]
   [auto-build.os.edn-utils.impl.reader :as build-impl-edn-reader]
   [auto-build.os.file                  :as build-file]))

(defn formatter-setup
  "Returns a map describing if zprint. "
  [{:keys [errorln normalln uri-str]
    :as _printers}]
  (let [zprint-cfg-filepath (build-file/expand-home-str "~/.zprintrc")
        home-setup-filedesc (->> zprint-cfg-filepath
                                 (build-impl-edn-reader/read-edn nil))
        {:keys [edn status]} home-setup-filedesc]
    (if-not (= status :success)
      (do (when normalln
            (normalln "zprint configuration is missing:"
                      ((if (fn? uri-str) uri-str identity) zprint-cfg-filepath)))
          {:status :fail
           :zprintrc home-setup-filedesc})
      (if (get edn :search-config?)
        {:status :success
         :zprintrc home-setup-filedesc}
        (do
          (when errorln
            (errorln
             "zprint local configuration is wrong. Please add `:search-config? true` in your `~/.zprintc`"))
          {:status :fail
           :zprintrc home-setup-filedesc})))))

(defn format-file-cmd
  "Command to format the `filepath` with zprint."
  [filepath]
  (when filepath ["zprint" "-w" (if (string? filepath) filepath (.getFile filepath))]))

(defn format-file
  [{:keys [normalln errorln uri-str]
    :as printers}
   app-dir
   filepath]
  (let [{:keys [message status]
         :as formatter}
        (formatter-setup printers)]
    (if (= status :fail)
      (do (errorln "Can't format file" (uri-str filepath) "as formatter is not setup properly:")
          (normalln message)
          {:status :formatter-not-setup
           :config-file formatter})
      (-> filepath
          format-file-cmd
          (build-cmd/print-on-error app-dir normalln errorln 10 100 100)
          (assoc :config-file formatter)))))
