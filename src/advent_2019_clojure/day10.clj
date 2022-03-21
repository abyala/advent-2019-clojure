(ns advent-2019-clojure.day10
  (:require
    [advent-2019-clojure.point :as point]
    [advent-2019-clojure.utils :as utils]))

(defn asteroid? [c] (= c \#))
(defn parse-asteroids [input]
  (transduce (comp (filter #(asteroid? (second %)))
                   (map first))
             conj
             #{}
             (point/parse-to-char-coords input)))

(def ^:private two-pi (* Math/PI 2))
(defn clock-radians [[x0 y0] [x1 y1]]
  (mod (Math/atan2 (- x1 x0) (- y0 y1))
       two-pi))

(defn lines-of-sight [source points]
  (let [comparator (fn [p1 p2] (apply compare (map (partial point/manhattan-distance source)
                                                   [p1 p2])))]
    (reduce (fn [m p] (utils/map-conj-sorted comparator m (clock-radians source p) p))
            {}
            points)))

(defn lines-of-sight-map [asteroids]
  (->> asteroids
       (map (fn [ast] [ast (lines-of-sight ast (disj asteroids ast))]))
       (into {})))

(defn best-station [lines-of-sight]
  (->> lines-of-sight
       (sort-by (comp count second))
       last
       second))

(defn part1 [input]
  (->> (parse-asteroids input)
       lines-of-sight-map
       best-station
       count))

(defn part2 [input]
  (let [vaporize-seq (->> (parse-asteroids input)
                          lines-of-sight-map
                          best-station
                          (sort-by first)
                          (map second)
                          (utils/interleave-all))
        [target-x target-y] (nth vaporize-seq 199)]
    (+ (* target-x 100) target-y)))
