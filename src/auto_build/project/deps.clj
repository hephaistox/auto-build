(ns auto-build.project.deps
  "Utilities for `deps.edn` files."
  (:refer-clojure :exclude [read])
  (:require
   [auto-build.os.edn-utils :as build-edn-utils]
   [auto-build.os.filename  :as build-filename]
   [babashka.fs             :as fs]))

;; ********************************************************************************
;; Deps file manipulation
;; ********************************************************************************

(defn deps-edn-filename
  "A `deps.edn` fullname based on the `app-dir` - the directory containing the application"
  [app-dir]
  (build-filename/create-file-path app-dir "deps.edn"))

(defn read
  "Returns a map with:
  * `:filepath`
  * `:afilepath` absolute path to the file
  * `:raw-content` if file can be read
  * `:invalid?` is boolean
  * `:exception` if something wrong happened
  * `:edn` if the translation is successful"
  [printers app-dir]
  (->> app-dir
       deps-edn-filename
       (build-edn-utils/read-edn printers)))

(comment
  (deps-edn-filename ".")
  ;
  (read {:errorln println
         :uri-str #(format "`%s`" %)
         :exception-msg #(println "Error: " %)}
        ".")
  ;
)

(defn write-edn
  "Returns a map with:
  * `:filepath` as given as a parameter
  * `:afilepath` absolute path to the file
  * `:status` is `:success` or `:fail`
  * `:raw-content`
  * `:exception` (only if `:status` is `:fail`)"
  [printers app-dir content]
  (build-edn-utils/write-edn (deps-edn-filename app-dir) printers content))

(comment
  (write-edn {:errorln println
              :uri-str #(format "`%s`" %)
              :exception-msg #(println "Error: " %)}
             "monorepo"
             {:a :b})
  ;
)

;; ********************************************************************************
;; Manipulate dependencies
;; ********************************************************************************

(defn extract-paths-to-deps
  "List of dependencies found in the `deps` map, is a list of pair :
  * The path to the deps
  * The dependency description"
  [deps]
  (->> (map (fn [[dep-alias {:keys [extra-deps]}]] [[:aliases dep-alias :extra-deps] extra-deps])
            (:aliases deps))
       (concat {[:deps] (:deps deps)})
       vec))

(defn is-hephaistox-project?
  [project-symbol]
  (re-find (re-pattern "com.github.hephaistox") (str project-symbol)))

(defn flatten-deps
  "Returns a map for each dependency. Its paths, dep-alias, dep-desc."
  [path-to-deps local-app-dir]
  (->> path-to-deps
       (mapcat (fn [[path dep-map]]
                 (keep (fn [[dep-alias dep]]
                         (when (is-hephaistox-project? dep-alias)
                           (let [{:hephaistox/keys [dir root]} dep]
                             {:is-local? (some? (:local/root dep))
                              :dir (-> (str (format "%s/%s" local-app-dir dir)
                                            (when root (str "/" root)))
                                       fs/normalize
                                       str)
                              :dep dep
                              :path (conj path dep-alias)
                              :dep-alias dep-alias})))
                       dep-map)))
       vec))

(comment
  (-> (read {:errorln println
             :uri-str #(format "`%s`" %)
             :exception-msg #(println "Error: " %)}
            "../auto_web/auto_web_cljc")
      :edn
      extract-paths-to-deps
      (flatten-deps "../auto_web/auto_web_cljc"))
  ;;
)

;; ********************************************************************************
;; Update dependnecies
;; ********************************************************************************

(defn update-deps-edn
  "Assign `value` at path `path` for the app in `app-dir`"
  [printers app-dir path value]
  (let [deps-filedesc (read printers app-dir)
        old-content (:edn deps-filedesc)
        content (assoc-in old-content path value)]
    (when-not (and (= :success (:status deps-filedesc)) (= old-content content))
      (write-edn printers app-dir content))))

(comment
  (update-deps-edn {:errorln println
                    :uri-str #(format "`%s`" %)
                    :exception-msg #(println "Error: " %)}
                   "landing"
                   [:deps 'com.github.hephaistox/auto-web]
                   {:local/root "../auto_web"})
  ;
)
