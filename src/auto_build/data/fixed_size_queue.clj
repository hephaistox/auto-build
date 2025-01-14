(ns auto-build.data.fixed-size-queue
  "Naive implementation of fixed length queue.

  A more efficient approach, if needed could be found in this [blog post](https://michaelrkytch.github.io/programming/clojure/2015/04/19/clj-buffer-efficiency.html) which is based on [clojure rrb](https://github.com/clojure/core.rrb-vector)."
  (:refer-clojure :exclude [pop enqueue peek]))

(defn init [size] {:iqueue '(), :size size})

(defn pop [queue] (update queue :iqueue butlast))

(defn enqueue
  [queue x]
  (let [{:keys [size iqueue]} queue
        queue (if (>= (count iqueue) size) (pop queue) queue)]
    (cond-> queue (pos? size) (update :iqueue conj x))))

(defn peek
  [queue]
  (-> queue
      :iqueue
      last))

(defn content
  [queue]
  (-> queue
      :iqueue
      vec))
(comment
  (-> (init 2)
      (enqueue :a)
      (enqueue :b)
      (enqueue :c)
      (enqueue :d)
      peek)
  ;
)
