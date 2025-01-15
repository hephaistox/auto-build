(ns auto-build.os.cli-opts-test
  (:require [auto-build.os.cli-opts :as sut]
            [clojure.test :refer [deftest is]]))

(deftest usage-test
  (is (= {:options {},
          :arguments [],
          :summary "  -h, --help  Print usage.",
          :errors nil}
         (sut/parse-cli-args [] sut/help-options))
      "Only one defaulted option is ok.")
  (is (= {:options {:help true},
          :arguments [],
          :summary "  -h, --help  Print usage.",
          :errors nil}
         (sut/parse-cli-args ["-h"] sut/help-options)))
  (is (= {:options {},
          :arguments [],
          :summary "  -h, --help  Print usage.",
          :errors ["Unknown option: \"-e\""]}
         (sut/parse-cli-args ["-e"] sut/help-options))
      "An unknown option is detected."))
