(ns advent-2019-clojure.utils
  (:require
    [clojure.math.numeric-tower :as math]
    [clojure.string :as str]))

(defn parse-long [s] (try
                       (Long/parseLong s)
                       (catch NumberFormatException _ nil)))

(defn abs [^long x] (Math/abs x))

(defn bytes->hex [b]
  (->> b
       (map (comp #(if (= 1 (count %)) (str "0" %) %)
                  #(Integer/toHexString %)
                  #(bit-and 0xff %)))
       (apply str)))

(defn map-add [m k n]
  (if (m k)
    (update m k + n)
    (assoc m k n)))

(defn map-conj
  ([m k v] (map-conj #(vector %) m k v))
  ([collection-supplier m k v] (if (m k)
                                 (update m k conj v)
                                 (assoc m k (collection-supplier v)))))

(defn map-conj-sorted [comparator m k v]
  (map-conj (partial sorted-set-by comparator) m k v))

(defn keep-indexes-when [f coll]
  (keep-indexed (fn [idx v] (when (f v) idx)) coll))

(defn split-blank-line
  "Given an input string, returns a sequence of sub-strings, separated by a completely
  blank string. This function preserves any newlines between blank lines, and it filters
  out Windows' \"\r\" characters."
  [input]
  (-> (str/replace input "\r" "")
      (str/split #"\n\n")))

(defn map-to-first-index [coll]
  (reduce (fn [m [idx v]] (if-not (m v) (assoc m v idx) m))
          {}
          (map-indexed vector coll)))

(defn interleave-all [colls]
  (when (seq colls)
    (lazy-seq (concat (map first colls)
                      (interleave-all (->> colls (map rest) (filter seq)))))))

(defn lcm-rf
  ([] 1)
  ([result] result)
  ([result input] (math/lcm result input)))
