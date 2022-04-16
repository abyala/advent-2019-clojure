(ns advent-2019-clojure.day12-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day12 :refer :all]))

(def test-data-1 "<x=-1, y=0, z=2>\n<x=2, y=-10, z=-7>\n<x=4, y=-8, z=8>\n<x=3, y=5, z=-1>")
(def test-data-2 "<x=-8, y=-10, z=0>\n<x=5, y=5, z=10>\n<x=2, y=-7, z=3>\n<x=9, y=-8, z=-3>")
(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (are [expected num-steps input] (= expected (part1 input num-steps))
                                  179 10 test-data-1
                                  1940 100 test-data-2
                                  7687 1000 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        2772 test-data-1
                        4686774924 test-data-2
                        334945516288044 puzzle-data))
