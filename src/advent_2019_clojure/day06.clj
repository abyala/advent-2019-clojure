(ns advent-2019-clojure.day06
  (:require
    [advent-2019-clojure.utils :as utils]
    [clojure.string :as str]))

(defn parse-orbits [input]
  (reduce (fn [m line]
            (let [[source orbiter] (str/split line #"\)")]
              (assoc m orbiter source)))
          {}
          (str/split-lines input)))

(defn path-to-com [orbits start]
  (->> start
       (iterate #(get orbits %))
       (take-while some?)
       rest))

(defn part1 [input]
  (let [orbits (parse-orbits input)]
    (transduce (map (comp count (partial path-to-com orbits) key))
               +
               orbits)))

(defn part2 [input]
  (let [orbits (parse-orbits input)
        from-san (utils/map-to-first-index (path-to-com orbits "SAN"))]
    (->> (path-to-com orbits "YOU")
         (keep-indexed (fn [idx v]
                         (when-let [d (from-san v)]
                           (+ d idx))))
         first)))