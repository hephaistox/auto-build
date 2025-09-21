(ns auto-build.tasks.deps-test
  (:require
   [auto-build.tasks.deps :as sut]
   [clojure.test          :refer [deftest is]]))

(deftest local-deps-test
  (is (empty? (sut/local-deps {'nrepl/nrepl {:mvn/version "1.3.1"}
                               'org.clojure/tools.cli {:mvn/version "1.1.230"}}))
      "No local root")
  (is (sut/local-deps {'nrepl/nrepl {:local/root "../aa"}
                       'org.clojure/tools.cli {:mvn/version "1.1.230"}})
      "A local root is detected, symbol is returned"))
