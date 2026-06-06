(ns auto-build.tasks.examples-test
  (:require
   [auto-build.tasks.examples :as sut]
   [clojure.test              :refer [deftest is testing]]))

(deftest parse-blocks-test
  (testing "A block followed by a #+RESULTS: keeps the `: `-stripped expected lines"
    (is (= [{:code "(println :a)\n(println :b)"
             :expected ["first" "second"]}]
           (sut/parse-blocks (str "#+begin_src clojure\n" "(println :a)\n"
                                  "(println :b)\n" "#+end_src\n"
                                  "\n" "#+RESULTS:\n"
                                  ": first\n" ": second\n")))))
  (testing "A block with no #+RESULTS: has an empty :expected"
    (is (= [{:code "(println :a)"
             :expected []}]
           (sut/parse-blocks "#+begin_src clojure\n(println :a)\n#+end_src\n"))))
  (testing "Several blocks are returned in order, non-clojure src ignored"
    (is (= [{:code "(println 1)"
             :expected ["1"]}
            {:code "(println 2)"
             :expected []}]
           (sut/parse-blocks (str
                              "some prose\n" "#+begin_src bash\necho skip\n#+end_src\n"
                              "#+begin_src clojure\n(println 1)\n#+end_src\n" "#+RESULTS:\n: 1\n"
                              "more prose\n" "#+begin_src clojure\n(println 2)\n#+end_src\n"))))))

(deftest chunks-of-test
  (is (= [["a" "b"] ["c"] []]
         (sut/chunks-of (str "a\nb\n" "__AUTO-BUILD-EXAMPLE-BLOCK__\n"
                             "c\n" "__AUTO-BUILD-EXAMPLE-BLOCK__")))
      "Splits stdout into one chunk per block on the delimiter"))

(deftest trim-blank-test
  (is (= ["a" "" "b"] (sut/trim-blank ["" "" "a" "" "b" "" ""]))
      "Leading/trailing blanks dropped, inner blanks kept"))

(deftest drift-test
  (testing "Empty expected means the block is not checked" (is (nil? (sut/drift [] ["anything"]))))
  (testing "Exact match returns nil" (is (nil? (sut/drift ["a" "b"] ["a" "b"]))))
  (testing "A line count mismatch reports a diff" (is (some? (sut/drift ["a"] ["a" "b"]))))
  (testing "A differing line reports a diff" (is (some? (sut/drift ["a" "b"] ["a" "X"]))))
  (testing "An expected line containing ... is a wildcard"
    (is (nil? (sut/drift ["count : ..."] ["count : 42"])))))
