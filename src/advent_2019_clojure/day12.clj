(ns advent-2019-clojure.day12
  (:require [advent-2019-clojure.utils :refer [parse-long abs]]
            [clojure.math.numeric-tower :as math]
            [clojure.string :as str]))

(def all-dimensions [:x :y :z])

(defn parse-moon [line]
  (->> line
       (re-seq #"-?\d+")
       (mapv (comp #(vector % 0) parse-long))
       (zipmap all-dimensions)))

(defn parse-input [input]
  (->> input str/split-lines (map parse-moon)))

(defn apply-gravity
  ([moon1 moon2] (reduce (fn [acc dim] (apply-gravity acc moon2 dim))
                         moon1
                         all-dimensions))
  ([moon1 moon2 dim] (->> (map (comp first dim) [moon2 moon1])
                           (apply compare)
                           (update-in moon1 [dim 1] +))))

(defn apply-gravities [moons]
  (map #(reduce apply-gravity % moons) moons))

(defn apply-velocity [moon]
  (reduce (fn [acc dim] (update-in acc [dim 0] + (get-in acc [dim 1])))
          moon
          all-dimensions))
(defn apply-velocities [moons]
  (map apply-velocity moons))

(defn take-step [moons]
  (-> moons apply-gravities apply-velocities))

(defn total-energy [moon]
  (letfn [(energy [f] (reduce + (map (comp abs f) (vals moon))))]
    (* (energy first) (energy second))))

(defn part1 [input num-steps]
  (let [moons (parse-input input)]
    (->> (nth (iterate take-step moons) num-steps)
         (map total-energy)
         (reduce +))))

(defn parse-input2 [input]
  (let [lines (str/split-lines input)
        num-lines (count lines)]
    (->> lines
         (mapv (fn [line] (->> line
                               (re-seq #"-?\d+")
                               (mapv parse-long))))
         (apply interleave)
         (partition num-lines)
         (map #(vector (vec %) (vec (repeat num-lines 0)))))))

(defn move-moons [positions velocities]
  (let [velocity-changes (map (comp (partial reduce +)
                                    (fn [pos] (map #(compare % pos) positions)))
                              positions)
        new-velocities (mapv + velocities velocity-changes)
        new-positions (mapv + positions new-velocities)]
    [new-positions new-velocities]))

(defn energy-calc [f moons]
  (->> moons
       (map f)
       (apply interleave)
       (map abs)
       (partition (count moons))
       (map (partial apply +))))
(def potential-energy2 (partial energy-calc first))
(def kinetic-energy2 (partial energy-calc second))

(defn total-energy2 [moons]
  (->> moons
       ((juxt potential-energy2 kinetic-energy2))
       (apply (partial map *))
       (reduce +)))

(defn moon-movement-seq [moons]
  (iterate (partial map (partial apply move-moons)) moons))

(defn part1-2 [input num-steps]
  (-> input parse-input2 moon-movement-seq (nth num-steps) total-energy2))

(defn first-cycle [dim-seq]
  (reduce (fn [seen state] (if (seen state)
                             (reduced (count seen))
                             (conj seen state)))
          #{}
          dim-seq))

(defn lcm-rf
  ([] 1)
  ([result] result)
  ([result input] (math/lcm result input)))

(defn part2 [input]
  (let [moon-seq (-> input parse-input2 moon-movement-seq)]
    (transduce (map (fn [dim] (->> moon-seq
                                   (map #(nth % dim))
                                   first-cycle)))
               lcm-rf
               (range 3))))
