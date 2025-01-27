(ns auto-build.code.formatter-test
  (:require
   [auto-build.code.formatter :as sut]
   [clojure.java.io           :as io]
   [clojure.test              :refer [deftest is testing]]))

(deftest format-file-cmd-test
  (is (->> (io/resource "to_be_formated.edn")
           (sut/format-file-cmd)
           last
           string?)
      "Is an url translated to string?"))

(deftest formatter-setup-test
  (testing "Returned values"
    (is (= {:status :success
            :zprintrc {:edn {:search-config? true}
                       :status :success}}
           (-> (sut/formatter-setup {:normalln println
                                     :errorln println})
               (update :zprintrc select-keys [:edn :status])
               (update-in [:zprintrc :edn] select-keys [:search-config?])))
        "Valid configuration file")
    #_(is (= {:status :fail
              :zprintrc {:edn {}
                         :status :success}}
             (-> (sut/formatter-setup nil)
                 (update :zprintrc select-keys [:edn :status])
                 (update-in [:zprintrc :edn] select-keys [:search-config?])))
          "Valid configuration file")
    #_(is (= {:status :fail
              :zprintrc {:edn {}
                         :status :fail}}
             (-> (sut/formatter-setup nil)
                 (update :zprintrc select-keys [:edn :status])
                 (update-in [:zprintrc :edn] select-keys [:search-config?])))
          "Valid configuration file"))
  (testing "What is printed"
    (is (= "" (with-out-str (sut/formatter-setup nil))) "What a valid formatter print")
    #_(is (= "zprint configuration is missing: /Users/anthonycaumond/.zprintrc\n"
             (with-out-str (sut/formatter-setup {:normalln println
                                                 :errorln println})))
          "For a missing file")
    #_(is
       (=
        "zprint local configuration is wrong. Please add `:search-config? true` in your `~/.zprintc`\n"
        (with-out-str (sut/formatter-setup {:normalln println
                                            :errorln println})))
       "For a wrong configuration")))


(deftest format-file-test
  (testing "Returned data"
    (is (= {:dir "."
            :cmd-str true
            :err-stream []
            :bb-proc true
            :out-stream []
            :status :success
            :adir true
            :config-file {:status :success
                          :zprintrc true}}
           (-> (sut/format-file nil "." (io/resource "to_be_formated.edn"))
               (update :cmd-str string?)
               (update :bb-proc some?)
               (update :adir string?)
               (update-in [:config-file :zprintrc] map?)
               (dissoc :cmd)))
        "Format an existing file")
    (is (= {:status :empty-cmd
            :config-file {:status :success}}
           (-> (sut/format-file nil "." (io/resource "non-existing-file"))
               (update :config-file select-keys [:status])))
        "Format a non existing file"))
  (testing "What is printed"
    (is (= ""
           (->> "to_be_formated.edn"
                io/resource
                (sut/format-file {:normalln println
                                  :errorln println}
                                 ".")
                with-out-str))
        "A successful file")
    #_(is (not= ""
                (->> "non-existing-file"
                     io/resource
                     (sut/format-file nil)
                     with-out-str))
          "A non existing file")
    #_(is (= "zez"
             (->> "invalid_content.edn"
                  io/resource
                  (sut/format-file nil)
                  with-out-str))
          "An invalid edn")))
