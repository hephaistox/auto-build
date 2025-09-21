(ns auto-build.os.file-test
  (:require
   [auto-build.os.file     :as sut]
   [auto-build.os.filename :as build-filename]
   [clojure.java.io        :as io]
   [clojure.string         :as str]
   [clojure.test           :refer [deftest is testing]]))

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
    (is (nil? (sut/is-existing-dir? "non-existing-file.edn")) "Not existing file")))

(deftest ensure-dir-exists-test
  (is (-> (sut/ensure-dir-exists (sut/create-temp-dir))
          sut/is-existing-dir?)
      "A created directory"))

(deftest delete-dir-test
  (let [tmp-dir (sut/create-temp-dir)]
    (sut/ensure-empty-dir tmp-dir)
    (is (= tmp-dir (sut/delete-dir tmp-dir)))
    (is (nil? (sut/delete-dir tmp-dir)) "Second deletion returns `nil`, as all non existing dir")))

(deftest ensure-empty-dir-test
  (is (let [tmp-dir (sut/create-temp-dir)] (= tmp-dir (sut/ensure-empty-dir tmp-dir)))))

(deftest copy-dir-test
  (comment
    (sut/copy-dir "src" "src2")))

;; ********************************************************************************
;; File manipulation

(deftest expand-home-str-test
  (is (not (str/includes? (sut/expand-home-str "~/.gitconfig") "~")) "Tilde is replaced.")
  (is (= "env/test/file_found.edn" (sut/expand-home-str "env/test/file_found.edn"))
      "No home is not replaced."))

(deftest is-existing-file?-test
  (testing "Empty dirs are considered has not existing"
    (is (= "." (sut/is-existing-file? "")))
    (is (= "." (sut/is-existing-file? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-file? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn")) "Not existing file"))
  (testing "Directories"
    (is (not (sut/is-existing-file? "src")) "An existing dir")
    (is (nil? (sut/is-existing-file? "non-existing-file.edn")) "Not existing dir")))

;; ********************************************************************************
;; Path manipulation

(deftest is-existing-path?-test
  (testing "Empty paths are considered has non existing"
    (is (= "." (sut/is-existing-path? "")))
    (is (= "." (sut/is-existing-path? nil))))
  (testing "File"
    (is (= "deps.edn" (sut/is-existing-path? "deps.edn")) "An existing file")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn")) "Not existing file"))
  (testing "Directories"
    (is (= "src" (sut/is-existing-path? "src")) "An existing dir")
    (is (nil? (sut/is-existing-path? "non-existing-file.edn")) "Not existing dir")))

(defn create-test-file
  [dir filename]
  (-> (build-filename/create-file-path dir filename)
      (sut/write-file nil filename)))

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
    (is (= #{{:path "a"
              :exist? true
              :type :file}
             {:path "b"
              :exist? true
              :type :file}
             {:path "c"
              :exist? true
              :type :directory}
             {:path "c/d"
              :exist? true
              :type :directory}
             {:path "c/d/e"
              :exist? true
              :type :directory}
             {:path "c/d/e/f"
              :exist? true
              :type :file}}
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

(deftest read-file-test
  (testing "Returned data"
    (is (= {:filepath "non-existing-file"
            :afilepath true
            :status :file-loading-fail}
           (-> (sut/read-file nil "non-existing-file")
               (dissoc :exception)
               (update :afilepath string?)))
        "Non existing file returns invalid?")
    (is (:exception (sut/read-file nil "non-existing-file")) "Non existing files contains errors"))
  (testing "Printing"
    (is (= ""
           (-> (sut/read-file {:errorln println
                               :exception-msg (comp println ex-message)}
                              (io/resource "valid_content.edn"))
               with-out-str))
        "What a valid file is printing")
    (is (str/includes? (-> (sut/read-file {:errorln println
                                           :exception-msg (comp println ex-message)}
                                          "non-existing-file.edn")
                           with-out-str)
                       "Impossible to load file non-existing-file.edn\n")
        "What an invalid file is printing")))

(deftest write-file-test
  (testing "Returned data"
    (is (= {:filepath true
            :afilepath true
            :raw-content "foo"
            :status :success}
           (-> (sut/create-temp-file)
               (sut/write-file nil "foo")
               (update :filepath string?)
               (update :afilepath string?)))
        "File properly written")
    (is (= #{:filepath :afilepath :raw-content :status}
           (-> (sut/create-temp-file)
               (sut/write-file nil "foo")
               keys
               set))
        "File properly written - Expected keys")
    (is (= {:filepath nil
            :afilepath true
            :raw-content "foo"
            :status :file-writing-fail}
           (-> nil
               (sut/write-file nil "foo")
               (update :afilepath string?)
               (dissoc :exception)))
        "An invalid filepath")
    (is (= #{:filepath :afilepath :exception :raw-content :status}
           (-> nil
               (sut/write-file nil "foo")
               keys
               set))
        "An invalid filepath - Expected keys"))
  (testing "What is printed"
    (is (= "tmp is updated\n"
           (->> (-> (sut/create-temp-file)
                    (sut/write-file {:normalln println} "foo")
                    with-out-str)
                (take-last 15)
                (apply str)))
        "What a successful file writing is printing")
    (is (= ""
           (-> (sut/create-temp-file)
               (sut/write-file {} "foo")
               with-out-str))
        "What a successful file writing is printing if no printer is provided")
    (is (str/includes? (-> nil
                           (sut/write-file {:errorln println
                                            :exception-msg (comp println ex-message)}
                                           "foo")
                           with-out-str)
                       "File nil could not be written\n")
        "What a failing file writing is printing")))

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
     (= #{{:relative-path "a"
           :type :file
           :options {:replace-existing true
                     :copy-attributes true}
           :exist? true}
          {:relative-path "b"
           :type :file
           :options {:replace-existing true
                     :copy-attributes true}
           :exist? true}
          {:relative-path "c"
           :type :directory
           :options {:replace-existing true
                     :copy-attributes true}
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
    (is (= #{"test2/a" "test2/b" "test2/c" "test2/c/d" "test2/c/d/e" "test2/c/d/e/f"}
           (->> (sut/search-files target-dir "**")
                (mapv #(build-filename/relativize % tmp-dir))
                set))
        "After the actual copy, all files are found in `d2`")))
