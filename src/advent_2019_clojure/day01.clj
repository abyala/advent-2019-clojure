(ns advent-2019-clojure.day01
  (:require
    [advent-2019-clojure.utils :refer [parse-long]]
    [clojure.string :as str]))

(defn fuel-required [mass]
  (-> mass (quot 3) (- 2)))

(defn recursive-fuel-required [mass]
  (->> (iterate fuel-required mass)
       rest
       (take-while pos-int?)
       (reduce +)))

(defn solve [calc input]
  (transduce (map (comp calc parse-long)) + (str/split-lines input)))

(def part1 (partial solve fuel-required))
(def part2 (partial solve recursive-fuel-required))
