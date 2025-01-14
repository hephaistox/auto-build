(ns auto-build.os.filename-test
  (:require [auto-build.os.filename :as sut]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(deftest remove-trailing-separator-test
  (is (str/blank? (sut/remove-trailing-separator nil)) "nil is a valid value")
  (is (str/blank? (sut/remove-trailing-separator "")) "nil is a valid value")
  (is (= "/foo" (sut/remove-trailing-separator "/foo")) "/foo")
  (is (= "/foo/bar" (sut/remove-trailing-separator "/foo/bar/"))
      "One trailing space")
  (is (= "/foo/bar" (sut/remove-trailing-separator "/foo/bar//"))
      "One trailing space"))

(deftest absolutize-test
  (is (string? (sut/absolutize "")))
  (is (string? (sut/absolutize nil))))

(deftest match-extension?-test
  (is (sut/match-extension? "file.txt" "txt") "valid")
  (is (not (sut/match-extension? nil "txt")) "empty is ok")
  (is (not (sut/match-extension? "file.org" "txt")) "not matching")
  (is (sut/match-extension? "file.org" "txt" "org")
      "not the first is matching"))
