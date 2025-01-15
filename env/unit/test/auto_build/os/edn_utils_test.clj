(ns auto-build.os.edn-utils-test
  (:require [auto-build.os.edn-utils :as sut]
            [auto-build.os.file :as build-file]
            [clojure.test :refer [deftest is]]))

(deftest read-edn-test
  (is (= {:filepath "non-existing-file", :exception true, :status :fail}
         (-> (sut/read-edn "non-existing-file")
             (update :exception some?)
             (dissoc :afilepath)))
      "Non existing file")
  (is (and (build-file/is-existing-file? "README.org")
           (not (= :success (:status (sut/read-edn "README.org")))))
      "Non edn file is skipped")
  (is (= #{:filepath :raw-content :exception :status :afilepath}
         (set (keys (sut/read-edn "README.org"))))
      "Non edn file returns exception and invalid?")
  (is (= #{:filepath :afilepath :raw-content :edn :status}
         (set (keys (sut/read-edn "deps.edn"))))
      "Existing edn file")
  (is (= #{:filepath :afilepath :exception :status}
         (set (keys (sut/read-edn "non-existing-file"))))
      "Non existing file"))
