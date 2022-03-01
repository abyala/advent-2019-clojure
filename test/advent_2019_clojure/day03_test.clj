(ns advent-2019-clojure.day03-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day03 :refer :all]))

(def test-data-1 "R75,D30,R83,U83,L12,D49,R71,U7,L72\nU62,R66,U55,R34,D71,R55,D58,R83")
(def test-data-2 "R98,U47,R26,D63,R33,U87,L62,D20,R33,U53,R51\nU98,R91,D20,R16,D67,R40,U7,R15,U6,R7")
(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        159  test-data-1
                        135 test-data-2
                        232 puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        610 test-data-1
                        410 test-data-2
                        6084 puzzle-data))