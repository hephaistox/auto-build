(ns auto-build.os.file-test
  (:require [auto-build.os.file :as sut]
            [auto-build.os.filename :as build-filename]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

;; ********************************************************************************
;; Directory manipulation

(deftest is-existing-dir?-test
  (testing "Empty dirs are considered current directory, so  existing"
    (is (= "." (sut/is-existing-dir? "")))
    (is (= "." (sut/is-existing-dir? nil))))
  (testing "Directories"
    (is (= "docs" (sut/is-existing-dir? "docs")) "An existing dir")
    (is (nil? (sut/is-existing-dir? "non-existing-dir")) "Not existing dir"))
  (testing "File"
    (is (not (sut/is-existing-dir? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-dir? "non-existing-file.edn"))
        "Not existing file")))

(deftest ensure-dir-exists-test
  (is (= "tmp/build-file-test" (sut/ensure-dir-exists "tmp/build-file-test"))
      "A created directory"))

(deftest delete-dir-test
  (sut/ensure-empty-dir "tmp/build-file-test")
  (is (= "tmp/build-file-test" (sut/delete-dir "tmp/build-file-test")))
  (is (nil? (sut/delete-dir "tmp/build-file-test"))))

(deftest ensure-empty-dir-test
  (is (= "tmp/build-file-test" (sut/ensure-empty-dir "tmp/build-file-test"))))

(deftest copy-dir-test
  (comment
    (sut/copy-dir "src" "src2")))

;; ********************************************************************************
;; File manipulation

(deftest expand-home-str-test
  (is (not (str/includes? (sut/expand-home-str "~/.gitconfig") "~"))
      "Tilde is replaced.")
  (is (= "env/test/file_found.edn"
         (sut/expand-home-str "env/test/file_found.edn"))
      "No home is not replaced."))

(deftest is-existing-file?-test
  (testing "Empty dirs are considered has not existing"
    (is (= "." (sut/is-existing-file? "")))
    (is (= "." (sut/is-existing-file? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-file? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn"))
        "Not existing file"))
  (testing "Directories"
    (is (not (sut/is-existing-file? "src")) "An existing dir")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn"))
        "Not existing dir")))

;; ********************************************************************************
;; Path manipulation

(deftest is-existing-path?-test
  (testing "Empty paths are considered has non existing"
    (is (= "." (sut/is-existing-path? "")))
    (is (= "." (sut/is-existing-path? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-path? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn"))
        "Not existing file"))
  (testing "Directories"
    (is (= "src" (sut/is-existing-path? "src")) "An existing dir")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn"))
        "Not existing dir")))

(defn create-test-file
  [dir filename]
  (-> (build-filename/create-file-path dir filename)
      (sut/write-file filename)))

(deftest path-on-disk?-test
  (let [dir (->> "test"
                 (build-filename/create-dir-path (sut/create-temp-dir))
                 sut/ensure-dir-exists)]
    (create-test-file dir "a")
    (create-test-file dir "b")
    (-> (build-filename/create-file-path dir "c" "d" "e")
        sut/ensure-dir-exists
        (create-test-file "f"))
    (is (= #{"a" "b" "c" "c/d" "c/d/e" "c/d/e/f"}
           (->> (sut/search-files dir "**" {})
                (map #(build-filename/relativize % dir))
                set))
        "Test the expected tree")
    (is (= #{{:path "a", :exist? true, :type :file}
             {:path "b", :exist? true, :type :file}
             {:path "c", :exist? true, :type :directory}
             {:path "c/d", :exist? true, :type :directory}
             {:path "c/d/e", :exist? true, :type :directory}
             {:path "c/d/e/f", :exist? true, :type :file}}
           (->> (sut/search-files dir "**" {})
                (mapv (fn [filename]
                        (-> (sut/path-on-disk filename)
                            (update :path #(build-filename/relativize % dir))
                            (dissoc :apath))))
                set))
        "Rich file list contains files/dirs, and nesting.")))

;; ********************************************************************************
;; Temporaries

(deftest create-temp-file-test
  (is (string? (sut/create-temp-file "test")))
  (is (-> (sut/create-temp-file "test")
          sut/is-existing-file?)))

(deftest create-temp-dir-test
  (is (string? (sut/create-temp-dir "test")))
  (is (-> (sut/create-temp-dir "test")
          sut/is-existing-dir?)))

;; ********************************************************************************
;; Modify file content

(deftest write-file-test
  (is (= {:filepath true, :afilepath true, :raw-content "foo", :status :success}
         (-> (sut/create-temp-file)
             (sut/write-file "foo")
             (update :filepath string?)
             (update :afilepath string?)))
      "File properly written")
  (is (= {:filepath nil, :afilepath true, :raw-content "foo", :status :fail}
         (-> nil
             (sut/write-file "foo")
             (update :afilepath string?)
             (dissoc :exception)))
      "File properly written")
  (is (= #{:filepath :afilepath :raw-content :status}
         (-> (sut/create-temp-file)
             (sut/write-file "foo")
             keys
             set))
      "Expected keys on error")
  (is (= #{:filepath :afilepath :exception :raw-content :status}
         (-> nil
             (sut/write-file "foo")
             keys
             set))
      "Expected keys on success"))

(deftest read-file-test
  (let [f (sut/read-file "non-existing-file")]
    (is (= {:filepath "non-existing-file", :afilepath true, :status :fail}
           (-> f
               (dissoc :exception)
               (update :afilepath string?)))
        "Non existing file returns invalid?")
    (is (:exception f) "Non existing files contains errors")))

;; ********************************************************************************
;; Search

(deftest copy-actions-test
  (let [tmp-dir (sut/create-temp-dir)
        dir (->> "test"
                 (build-filename/create-dir-path tmp-dir)
                 sut/ensure-dir-exists)
        target-dir (->> "test2"
                        (build-filename/create-dir-path tmp-dir)
                        sut/ensure-dir-exists)]
    (create-test-file dir "a")
    (create-test-file dir "b")
    (-> (build-filename/create-file-path dir "c" "d" "e")
        sut/ensure-dir-exists
        (create-test-file "f"))
    (is
      (= #{{:relative-path "a",
            :type :file,
            :options {:replace-existing true, :copy-attributes true},
            :exist? true}
           {:relative-path "b",
            :type :file,
            :options {:replace-existing true, :copy-attributes true},
            :exist? true}
           {:relative-path "c",
            :type :directory,
            :options {:replace-existing true, :copy-attributes true},
            :exist? true}}
         (->> (sut/search-files dir "*")
              (mapv #(-> %
                         (sut/copy-action dir target-dir)
                         (select-keys [:relative-path :type :options :exist?])))
              set)))))

(deftest do-copy-action-test
  (let [tmp-dir (sut/create-temp-dir)
        dir (->> "test"
                 (build-filename/create-dir-path tmp-dir)
                 sut/ensure-dir-exists)
        target-dir (->> "test2"
                        (build-filename/create-dir-path tmp-dir)
                        sut/ensure-dir-exists)]
    (create-test-file dir "a")
    (create-test-file dir "b")
    (-> (build-filename/create-file-path dir "c" "d" "e")
        sut/ensure-dir-exists
        (create-test-file "f"))
    (is (= []
           (->> (sut/search-files dir "*")
                (mapv #(-> %
                           (sut/copy-action dir target-dir)
                           sut/do-copy-action))
                (filter #(not= :success (:status %)))))
        "No error happens")
    (is (= #{"test2/a" "test2/b" "test2/c" "test2/c/d" "test2/c/d/e"
             "test2/c/d/e/f"}
           (->> (sut/search-files target-dir "**")
                (mapv #(build-filename/relativize % tmp-dir))
                set))
        "After the actual copy, all files are found in `d2`")))
