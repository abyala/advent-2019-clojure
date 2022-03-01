(ns advent-2019-clojure.day03
  (:require
    [advent-2019-clojure.point :as point]
    [advent-2019-clojure.utils :refer [parse-long]]
    [clojure.string :as str]))


(defn parse-directions [input]
  (->> (str/split input #",")
       (map (fn [s] [({\U :up, \D :down, \L :left, \R :right} (first s))
                     (parse-long (subs s 1))]))))

(def direction-map {:up    point/move-up
                    :down  point/move-down
                    :left  point/move-left
                    :right point/move-right})

(defn points-from [point direction distance]
  (let [f (direction-map direction)]
    (->> (iterate f point)
         rest
         (take distance))))

(defn walk-path [directions]
  (->> directions
       (reduce (fn [[point path] [direction distance]]
                 (let [next-path (points-from point direction distance)]
                   [(last next-path) (apply conj path next-path)]))
               [point/origin []])
       second))

(defn first-time-at-points [points]
  (reduce (fn [acc [p dist]] (if (acc p) acc (assoc acc p dist)))
          {}
          (map list points (rest (range)))))

(defn solve [f input]
  (->> (str/split-lines input)
       (map (comp first-time-at-points walk-path parse-directions))
       (apply merge-with list)
       (filter #(list? (second %)))
       (map f)
       (apply min)))

(def part1 (partial solve (comp point/manhattan-distance first)))
(def part2 (partial solve (comp (partial apply +) second)))
