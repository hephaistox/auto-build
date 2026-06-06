(ns auto-build.tasks.examples
  "Run doc examples.

  Each `.org` file may hold several `#+begin_src clojure` blocks. All blocks of a
  file run in one JVM (state is shared, as in an org-babel session) and each
  block's stdout is checked against the `#+RESULTS:` that immediately follows it.
  Fails if any block throws or its stdout drifts from the expected result.

  The project provides *which* files to check (`org-files`) and *how* to run a
  block file (`block-cmd`); everything else - org parsing, the stdout/`#+RESULTS:`
  diffing, the `...` wildcard - is handled here."
  (:require
   [auto-build.os.cli-opts :as build-cli-opts]
   [auto-build.os.cmd      :as build-cmd]
   [clojure.java.io        :as io]
   [clojure.java.shell     :as cjsh]
   [clojure.string         :as str]))

;; ********************************************************************************
;; *** Task setup
;; ********************************************************************************

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli-args))

;; A delimiter `println`-ed between blocks so the captured stdout can be split
;; back into one chunk per block. Unlikely to collide with example output.
(def ^:private delim "__AUTO-BUILD-EXAMPLE-BLOCK__")
(def ^:private temp-prefix "auto-build-example")

;; ********************************************************************************
;; *** Org parsing & diffing (pure)
;; ********************************************************************************

(defn parse-blocks
  "Parse an `.org` `content` into an ordered vector of `{:code .. :expected [lines]}`.
  `:expected` is the `: `-stripped lines of the `#+RESULTS:` that immediately
  follows the block (empty vector when the block has none)."
  [content]
  (loop [ls (str/split-lines content)
         blocks []]
    (if (empty? ls)
      blocks
      (if (str/starts-with? (str/triml (first ls)) "#+begin_src clojure")
        (let [[code after] (split-with (fn [x] (not (str/starts-with? (str/triml x) "#+end_src")))
                                       (rest ls))
              post-blank (drop-while str/blank? (rest after))
              results? (and (seq post-blank) (= "#+RESULTS:" (str/trim (first post-blank))))
              [expected remaining]
              (if results?
                (let [[exp tail] (split-with (fn [x] (str/starts-with? (str/triml x) ":"))
                                             (rest post-blank))]
                  [(mapv (fn [x] (str/replace-first (str/triml x) (re-pattern "^:\\s?") "")) exp)
                   tail])
                [[] post-blank])]
          (recur remaining
                 (conj blocks
                       {:code (str/join "\n" code)
                        :expected expected})))
        (recur (rest ls) blocks)))))

(defn chunks-of
  "Split captured stdout `out` back into one line-vector per block (on the delimiter)."
  [out]
  (reduce (fn [acc line] (if (= line delim) (conj acc []) (update acc (dec (count acc)) conj line)))
          [[]]
          (str/split-lines out)))

(defn trim-blank
  "Drop leading and trailing blank lines from `lines`."
  [lines]
  (->> lines
       (drop-while str/blank?)
       reverse
       (drop-while str/blank?)
       reverse))

(defn drift
  "`nil` when `actual` matches `expected`; otherwise a readable diff string. An
  expected line containing `...` is a wildcard (shape-described, not pinned)."
  [expected actual]
  (cond
    (empty? expected) nil
    (not= (count expected) (count actual)) (str "  line count: expected " (count expected)
                                                ", got " (count actual)
                                                "\n  expected:\n    " (str/join "\n    " expected)
                                                "\n  actual:\n    " (str/join "\n    " actual))
    :else (some (fn [[i e a]]
                  (when-not (or (str/includes? e "...") (= (str/trimr e) (str/trimr a)))
                    (str "  line " (inc i) ":\n    expected: " e "\n    actual:   " a)))
                (map vector (range) expected actual))))

;; ********************************************************************************
;; *** Task code
;; ********************************************************************************

(defn- run-file
  "Run one example file `f` and check it. Returns `:success` or `:fail`."
  [{:keys [normalln errorln subtitle]
    :as _printers}
   app-dir
   block-cmd
   f]
  (let [path (.getPath (io/file f))
        blocks (parse-blocks (slurp f))]
    (subtitle (str "Running " path " (" (count blocks) " block(s))"))
    (if (empty? blocks)
      (do (errorln (str "FAILED: no clojure src block in " path)) :fail)
      (let [tmp (java.io.File/createTempFile temp-prefix ".clj")
            _ (spit tmp (str/join (str "\n(println \"" delim "\")\n") (map :code blocks)))
            {:keys [exit out err]} (apply cjsh/sh
                                          (concat (block-cmd (.getPath tmp)) [:dir app-dir]))]
        (.delete tmp)
        (doseq [line (remove (fn [l] (= l delim)) (str/split-lines out))] (normalln line))
        (if-not (zero? exit)
          (do (errorln err) (errorln (str "FAILED (threw): " path)) :fail)
          (or (some (fn [[idx block chunk]]
                      (when-let [problem (drift (:expected block) (trim-blank chunk))]
                        (errorln (str "FAILED (output drift): " path " - block " (inc idx)))
                        (errorln problem)
                        :fail))
                    (map vector (range) blocks (chunks-of out)))
              :success))))))

(defn examples*
  "Run every file in `org-files`. `block-cmd` is `(fn [tmp-path] -> cmd-vector)`
  building the command that runs the temp `.clj` holding a file's blocks.
  Fails fast: returns `{:status :fail}` on the first failing file."
  [{:keys [errorln normalln]
    :as printers}
   app-dir
   org-files
   block-cmd]
  (if (empty? org-files)
    (do (errorln "No example files to run") {:status :fail})
    (loop [[f & fs] org-files]
      (if (nil? f)
        (do (normalln
             (str "All " (count org-files) " example files ran (stdout matches #+RESULTS:)."))
            {:status :success})
        (if (= :fail (run-file printers app-dir block-cmd f)) {:status :fail} (recur fs))))))

(defn examples
  "Run the doc examples in `org-files`, checking each block's stdout against its
  `#+RESULTS:`. Returns an exit code."
  [{:keys [title]
    :as printers}
   app-dir
   current-task
   org-files
   block-cmd]
  (if-let [exit-code (build-cli-opts/enter cli-opts current-task)]
    exit-code
    (let [title-msg "Doc examples"]
      (title title-msg)
      (-> (examples* printers app-dir org-files block-cmd)
          (build-cmd/status-to-exit-code printers title-msg)))))
