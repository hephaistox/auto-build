(ns compile
  (:require
   [clojure.tools.build.api :refer [compile-clj create-basis jar]]))

(defn compile-jar
  [{:keys [target-dir]
    :as _pars}]
  (let [class-dir (str target-dir "/classes")
        jar-file (str target-dir "/production/auto_build.jar")
        basis (create-basis)]
    (try (let [compile (compile-clj {:basis basis
                                     :bindings {#'clojure.core/*assert* false
                                                #'clojure.core/*warn-on-reflection* true}
                                     :out :capture
                                     :err :capture
                                     :class-dir class-dir})]
           (jar {:class-dir class-dir
                 :jar-file jar-file})
           {:compile-jar compile})
         (catch Exception e
           {:compile-jar {:exception e}
            :status :compilation-failed}))))
