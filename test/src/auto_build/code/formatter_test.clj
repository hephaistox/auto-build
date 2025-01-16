(ns auto-build.code.formatter-test
  (:require [auto-build.code.formatter :as sut]
            [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]))

(deftest format-file-cmd-test
  (is (->> (io/resource "to_be_formated.edn")
           (sut/format-file-cmd)
           last
           string?)
      "Is an url translated to string?"))
