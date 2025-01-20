(ns auto-build.code.vcs-test
  (:require [clojure.test :refer [deftest #_is testing]]
            #_[auto-build.code.vcs :as sut]))

(deftest latest-commit-message-test
  #_(is (= {:status :success, :git-describe {:status :success}, :tag "wip"}
           (-> (sut/latest-commit-message {:normalln println, :errorln println}
                                          ""
                                          false)
               (update :git-describe select-keys [:status])))))

(deftest latest-commit-sha-test
  #_(is (= {:status :success,
            :git-describe {:status :success},
            :commit-sha "3b1fbd8dfba397f4761f062388d9252cf043a7bf"}
           (-> (sut/latest-commit-sha {:normalln println, :errorln println}
                                      ""
                                      false)
               (update :git-describe select-keys [:status])))))

(deftest current-branch-test
  #_(is (= {:status :success,
            :git-branch {:status :success},
            :branch-name "deployment"}
           (->
             (sut/current-branch {:normalln println, :errorln println} "" false)
             (update :git-branch select-keys [:status])))))

(deftest clean-state-test
  (testing
    "That tests are removed as they should be executed on dev environement and are dependant on the state"
    #_(with-out-str
        (testing "In a dirty status"
          (is (= {:status :dirty-state, :git-status {:status :success}}
                 (-> (sut/clean-state {:normalln println, :errorln println}
                                      "."
                                      true)
                     (update :git-status select-keys [:status])))
              "Returned data")
          (is (= "Execute [git status -s]\n"
                 (with-out-str (sut/clean-state {:normalln println,
                                                 :errorln println}
                                                "."
                                                true))))))
    #_(testing "In a clean status"
        (is (= :success
               (:status (sut/clean-state {:normalln println, :errorln println}
                                         "."
                                         true)))
            "When the status")
        (is (= "Execute [git status -s]\n"
               (with-out-str (sut/clean-state {:normalln println,
                                               :errorln println}
                                              "."
                                              true)))))))

(deftest nothing-to-push-test
  (testing
    "That tests are removed as they should be executed on dev environement and are dependant on the state"
    #_(with-out-str
        (testing "In a dirty status"
          (is (= {:status :not-pushed, :git-status {:status :success}}
                 (-> (sut/nothing-to-push {:normalln println, :errorln println}
                                          "."
                                          true)
                     (update :git-status select-keys [:status])))
              "Returned data")
          (is (= "Execute [git status]\n"
                 (with-out-str (sut/nothing-to-push {:normalln println,
                                                     :errorln println}
                                                    "."
                                                    true))))))
    #_(testing "In a clean status"
        (is (= :success
               (:status (sut/nothing-to-push {:normalln println,
                                              :errorln println}
                                             "."
                                             true)))
            "When the status")
        (is (= "Execute [git status]\n"
               (with-out-str (sut/nothing-to-push {:normalln println,
                                                   :errorln println}
                                                  "."
                                                  true)))))))

(deftest current-tag-test
  #_(is (= {:status :cmd-failed,
            :git-describe
              {:status :fail,
               :out-stream [],
               :err-stream
                 ["fatal: No names found, cannot describe anything."]}}
           (-> (sut/current-tag {:errorln println, :normalln println} "" false)
               (update :git-describe
                       select-keys
                       [:status :out-stream :err-stream])))
        "A brand new repo"))

(deftest current-repo-url-test
  #_(is
      (= {:status :success,
          :git-remote
            {:status :success,
             :out-stream
               ["origin\tgit@github.com:hephaistox/auto-build.git (push)"
                "origin\tgit@github.com:hephaistox/auto-build.git (fetch)"],
             :err-stream []},
          :repo-url "git@github.com:hephaistox/auto-build.git"}
         (->
           (sut/current-repo-url {:errorln println, :normalln println} "" false)
           (update :git-remote select-keys [:status :out-stream :err-stream])))
      "For auto-build repo"))

(deftest gh-run-wip-test
  #_(is (= {:status :success,
            :gh-run-list {:status :success},
            :last-run {:run-id true, :status :run-ok}}
           (-> (sut/gh-run-wip? {:normalln println, :errorln println} "" true)
               (update :gh-run-list select-keys [:status])
               (update-in [:last-run :run-id] string?)))
        "When last run is succesful"))

(deftest gh-run-view-test
  #_(is
      (=
        {:status :success,
         :gh-run-view {:status :success},
         :run-id 12776650208,
         :message
           "View this run on GitHub: https://github.com/hephaistox/auto-build/actions/runs/12776650208"}
        (-> (sut/gh-run-view {:normalln println, :errorln println}
                             ""
                             true
                             12776650208)
            (update :gh-run-view select-keys [:status])))))
