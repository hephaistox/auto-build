(ns auto-build.project.map
  "The project map gathers all useful informations for a project"
  (:require [auto-build.project.config :as build-project-config]
            [auto-build.project.deps :as build-deps]))

(defn create-project-map
  "Creates a project map based on app in `app-dir`."
  [app-dir]
  {:app-dir app-dir})

(defn add-project-config
  "Adds project configuration file desc `project-config-filedesc`to the `project-map`."
  [printers {:keys [app-dir], :as project-map}]
  (let [project-config-filedesc (build-project-config/read printers app-dir)]
    (assoc project-map
      :project-config-filedesc project-config-filedesc
      :app-name (get-in project-config-filedesc [:edn :app-name]))))

(defn add-deps-edn
  "Adds `:deps` key to to project map with the `deps.edn` file descriptor."
  [printers {:keys [app-dir], :as project-map}]
  (assoc project-map :deps (build-deps/read printers app-dir)))
