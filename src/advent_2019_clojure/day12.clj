(ns advent-2019-clojure.day12
  (:require [advent-2019-clojure.utils :refer [parse-long abs lcm-rf]]
            [clojure.string :as str]))

(defn parse-input [input]
  (let [lines (str/split-lines input)
        num-lines (count lines)]
    (->> lines
         (map (fn [line] (->> line
                              (re-seq #"-?\d+")
                              (mapv parse-long))))
         (apply interleave)
         (partition num-lines)
         (map #(vector (vec %) (vec (repeat num-lines 0)))))))

(defn move-moons [[positions velocities]]
  (let [velocity-changes (map (comp (partial reduce +)
                                    (fn [pos] (map #(compare % pos) positions)))
                              positions)
        new-velocities (mapv + velocities velocity-changes)
        new-positions (mapv + positions new-velocities)]
    [new-positions new-velocities]))

(defn moon-movement-seq [moons]
  (iterate #(map move-moons %) moons))

(defn energy-calc [f moons]
  (->> moons
       (map f)
       (apply interleave)
       (map abs)
       (partition (count moons))
       (map (partial apply +))))
(defn potential-energy [moons] (energy-calc first moons))
(defn kinetic-energy   [moons] (energy-calc second moons))

(defn total-energy [moons]
  (->> moons
       ((juxt potential-energy kinetic-energy))
       (apply (partial map *))
       (reduce +)))

(defn part1 [input num-steps]
  (-> input parse-input moon-movement-seq (nth num-steps) total-energy))

(defn first-cycle [dim-seq]
  (reduce (fn [seen state] (if (seen state)
                             (reduced (count seen))
                             (conj seen state)))
          #{}
          dim-seq))

(defn part2 [input]
  (let [moon-seq (-> input parse-input moon-movement-seq)]
    (transduce (map (fn [dim] (->> moon-seq
                                   (map #(nth % dim))
                                   first-cycle)))
               lcm-rf
               (range 3))))