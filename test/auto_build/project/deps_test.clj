(ns auto-build.project.deps-test
  (:require
   [auto-build.project.deps :as sut]
   [clojure.test            :refer [deftest is]]))

(deftest flatten-deps-test
  (is (= [{:path [:deps 'com.github.hephaistox/auto-web-cljs]
           :dep-alias 'com.github.hephaistox/auto-web-cljs
           :is-local? true
           :dir "../auto_web"
           :dep #:local{:root "../auto_web/"}}]
         (sut/flatten-deps [[[:deps]
                             {'com.github.hephaistox/auto-web-cljs {:local/root "../auto_web/"}}]]
                           "."))
      "A repo with :local/root")
  (is (= [{:path [:deps 'com.github.hephaistox/auto-web-clj]
           :dep-alias 'com.github.hephaistox/auto-web-clj
           :dep {:deps/root "auto_web_clj"
                 :git/deps 'com.github.hephaistox/auto-web
                 :git/sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"
                 :git/url "https://github.com/hephaistox/auto-web"}
           :is-local? false
           :dir "../auto_web/auto_web_clj"}]
         (sut/flatten-deps [[[:deps]
                             {'com.github.hephaistox/auto-web-clj
                              {:deps/root "auto_web_clj"
                               :git/deps 'com.github.hephaistox/auto-web
                               :git/sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"
                               :git/url "https://github.com/hephaistox/auto-web"}}]]
                           "."))
      "A repo defined with url and root")
  (is (= [{:path [:deps 'com.github.hephaistox/auto-core]
           :dep-alias 'com.github.hephaistox/auto-core
           :dep {:git/deps 'com.github.hephaistox/auto-core
                 :git/sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"
                 :git/url "https://github.com/hephaistox/auto-core"}
           :is-local? false
           :dir "../auto_core"}]
         (sut/flatten-deps [[[:deps]
                             {'com.github.hephaistox/auto-core
                              {:git/deps 'com.github.hephaistox/auto-core
                               :git/sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"
                               :git/url "https://github.com/hephaistox/auto-core"}}]]
                           "."))
      "A repo defined with url, not using any root")
  (is (= [{:is-local? false
           :dir "../auto_web"
           :dep #:git{:sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"}
           :path [:deps 'com.github.hephaistox/auto-web]
           :dep-alias 'com.github.hephaistox/auto-web}]
         (sut/flatten-deps [[[:deps]
                             {'com.github.hephaistox/auto-web
                              {:git/sha "51070f5793bd5c3526a1d5812ac9ded1bfceafeb"}}]]
                           "."))
      "A repo with git/sha only"))
