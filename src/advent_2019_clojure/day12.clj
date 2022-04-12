(ns advent-2019-clojure.day12
  (:require [advent-2019-clojure.utils :refer [parse-long abs]]
            [clojure.math.combinatorics :as combo]
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
  ([moons] (map #(reduce apply-gravity % moons) moons))
  ([moon1 moon2] (reduce (fn [acc prop] (apply-gravity acc moon2 prop))
                         moon1
                         all-dimensions))
  ([moon1 moon2 prop] (->> (map (comp first prop) [moon2 moon1])
                           (apply compare)
                           (update-in moon1 [prop 1] +))))

(defn apply-velocity [moon]
  (reduce (fn [acc prop] (update-in acc [prop 0] + (get-in acc [prop 1])))
          moon
          all-dimensions))
(defn apply-velocities [moons]
  (map apply-velocity moons))

(defn take-step [moons]
  (-> moons apply-gravity apply-velocities))

(defn potential-energy [moon]
  (reduce + (map (comp abs first) (vals moon))))

(defn kinetic-energy [moon]
  (reduce + (map (comp abs second) (vals moon))))

(defn total-energy [moon]
  (* (potential-energy moon) (kinetic-energy moon)))

(defn part1 [input num-steps]
  (let [moons (parse-input input)]
    (->> (nth (iterate take-step moons) num-steps)
         (map total-energy)
         (reduce +))))

(defn part2-will-not-complete [input]
  (reduce (fn [seen state] (if (seen state)
                             (reduced (count seen))
                             (conj seen state)))
          #{}
          (iterate take-step (parse-input input))))
