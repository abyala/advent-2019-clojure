(ns advent-2019-clojure.day09-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day09 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (is (= 2671328082 (part1 puzzle-data))))

(deftest part2-test
  (= 59095 (part2 puzzle-data)))
