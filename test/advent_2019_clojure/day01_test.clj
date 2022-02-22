(ns advent-2019-clojure.day01-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day01 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest fuel-required-test
  (are [mass fuel] (= fuel (fuel-required mass))
                   12 2
                   14 2
                   1969 654
                   100756 33583))

(deftest part1-test
  (is (= 3454026 (part1 puzzle-data))))

(deftest recursive-fuel-required-test
  (are [mass fuel] (= fuel (recursive-fuel-required mass))
                   14 2
                   1969 966
                   100756 50346))

(deftest part2-test
  (is (= 5178170 (part2 puzzle-data))))
