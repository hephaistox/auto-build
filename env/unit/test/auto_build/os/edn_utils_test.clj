(ns auto-build.os.edn-utils-test
  (:require
   [auto-build.os.edn-utils :as sut]
   [auto-build.os.file      :as build-file]
   [clojure.java.io         :as io]
   [clojure.string          :as str]
   [clojure.test            :refer [deftest is testing]]))

(deftest read-edn-test
  (testing "Check returned data"
    (is (= {:filedesc {:filepath "non-existing-file"
                       :exception true
                       :afilepath true
                       :status :file-loading-fail}
            :status :file-loading-fail}
           (-> (sut/read-edn nil "non-existing-file")
               (update-in [:filedesc :exception] some?)
               (update-in [:filedesc :afilepath] string?)))
        "Non existing file")
    (is (= {:filedesc {:filepath true
                       :afilepath true
                       :status :success
                       :raw-content true}
            :exception true
            :status :edn-failed}
           (-> (sut/read-edn nil (io/resource "invalid_content.edn"))
               (update-in [:filedesc :filepath] string?)
               (update-in [:filedesc :afilepath] string?)
               (update-in [:filedesc :raw-content] string?)
               (update :exception some?)))
        "Non edn file returns exception and a non valid status")
    (is (= {:filedesc {:filepath true
                       :afilepath true
                       :raw-content "{:a :b}\n"
                       :status :success}
            :status :success
            :edn {:a :b}}
           (-> (sut/read-edn nil (io/resource "valid_content.edn"))
               (update-in [:filedesc :filepath] string?)
               (update-in [:filedesc :afilepath] string?)))
        "Existing edn file"))
  (testing "What is printed"
    (is (str/includes? (with-out-str (sut/read-edn {:errorln println
                                                    :exception-msg #(println (ex-message %))}
                                                   "non-existing-file"))
                       "No such file or directory)\n")
        "For a non existing file")
    (is (str/includes? (with-out-str (sut/read-edn {:errorln println
                                                    :exception-msg #(println (ex-message %))}
                                                   (io/resource "invalid_content.edn")))
                       "is not a valid edn.\nMap literal must contain an even number of forms\n")
        "For a content which is not a valid edn")
    (is (= ""
           (with-out-str (sut/read-edn {:errorln println
                                        :exception-msg #(println (ex-message %))}
                                       (io/resource "valid_content.edn"))))
        "For a valid file")))

(deftest write-edn-test
  (testing "Returned data"
    (is (= {:edn-filepath true
            :status :success
            :filedesc {:filepath true
                       :raw-content "foo"
                       :status :success}
            :formatting {:status :success}}
           (-> (sut/write-edn (build-file/create-temp-file) nil "foo")
               (update :edn-filepath string?)
               (update :filedesc #(select-keys % [:filepath :raw-content :status]))
               (update-in [:filedesc :filepath] string?)
               (update :formatting #(select-keys % [:status]))))
        "File properly written")
    (is (= {:edn-filepath nil
            :status :file-writing-fail
            :filedesc {:raw-content "foo"
                       :exception true
                       :status :file-writing-fail}}
           (-> (sut/write-edn nil nil "foo")
               (update :filedesc select-keys [:status :exception :raw-content])
               (update-in [:filedesc :exception] some?)
               (dissoc :exception)))
        "An invalid filepath"))
  (testing "What is printed"
    (is (= "tmp is updated\n"
           (->> (-> (build-file/create-temp-file)
                    (sut/write-edn {:normalln println} "foo")
                    with-out-str)
                (take-last 15)
                (apply str)))
        "What a successful file writing is printing")
    (is (= ""
           (-> (build-file/create-temp-file)
               (sut/write-edn {} "foo")
               with-out-str))
        "What a successful file writing is printing if no printer is provided")
    (is (= "File nil could not be written\nCannot open <nil> as a Writer.\n"
           (-> nil
               (sut/write-edn {:errorln println
                               :exception-msg (comp println ex-message)}
                              "foo")
               with-out-str))
        "What a failing file writing is printing")))
