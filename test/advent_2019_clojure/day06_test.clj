(ns advent-2019-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day06 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (is (= 315757 (part1 puzzle-data))))

(deftest part2-test
  (is (= 481 (part2 puzzle-data))))
