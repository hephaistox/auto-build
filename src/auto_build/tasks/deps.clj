(ns auto-build.tasks.deps
  (:require
   [auto-build.os.exit-codes]
   [auto-build.project.bb   :as pb]
   [auto-build.project.deps :as pd]))

(defn read-bb
  [printers app-dir]
  (-> (pb/read printers app-dir)
      :edn))

(defn read-deps
  [printers app-dir]
  (-> (pd/read printers app-dir)
      :edn))

(defn local-deps
  "Return local dependencies"
  [deps]
  (-> (filter #(= :local/root
                  (-> %
                      second
                      ffirst))
              deps)
      keys))

(defn extract-deps-to-check
  [printers app-dir]
  (let [bb-content (read-bb printers app-dir)
        deps-content (read-deps printers app-dir)]
    (concat [{:path ["bb.edn"]
              :deps (:deps bb-content)}
             {:path ["deps.edn"]
              :deps (:deps deps-content)}]
            (keep (fn [[alias alias-deps]]
                    (when (:deps alias-deps)
                      {:path ["deps.edn" :aliases alias :deps]
                       :deps (:deps alias-deps)}))
                  (:aliases deps-content))
            (keep (fn [[alias alias-deps]]
                    (when (:extra-deps alias-deps)
                      {:path ["deps.edn" :aliases alias :extra-deps]
                       :deps (:extra-deps alias-deps)}))
                  (:aliases deps-content)))))

(comment
  (extract-deps-to-check {:errorln println
                          :uri-str #(format "`%s`" %)
                          :exception-msg #(println "Error: " %)}
                         ".")
  ;
)

(defn task
  [{:keys [errorln subtitle uri-str]
    :as printers}
   app-dir]
  (first (keep (fn [{:keys [path deps]}]
                 (if-let [local-dep-found (seq (local-deps deps))]
                   (do (subtitle "Local deps are found in" path)
                       (doseq [local-dep local-dep-found]
                         (errorln (uri-str local-dep) "refers to a local repo"))
                       auto-build.os.exit-codes/general-errors)
                   (do (subtitle "Deps refers to remote repo in" path) nil)))
               (extract-deps-to-check printers app-dir))))

(comment
  (read-bb {:errorln println
            :uri-str #(format "`%s`" %)
            :exception-msg #(println "Error: " %)}
           ".")
  (read-deps {:errorln println
              :uri-str #(format "`%s`" %)
              :exception-msg #(println "Error: " %)}
             ".")
  ;
)
