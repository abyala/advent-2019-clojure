(ns advent-2019-clojure.day07-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day07 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (is (= 18812 (part1 puzzle-data))))

#_(deftest part2-test
  (is (= 14110739 (part2 puzzle-data))))
