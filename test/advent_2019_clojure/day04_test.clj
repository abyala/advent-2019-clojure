(ns advent-2019-clojure.day04-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.day04 :refer :all]))

(def puzzle-min 240298)
(def puzzle-max 784956)

(deftest part1-test
  (is (= 1150 (part1 puzzle-min puzzle-max))))

(deftest part2-test
  (is (= 748 (part2 puzzle-min puzzle-max))))