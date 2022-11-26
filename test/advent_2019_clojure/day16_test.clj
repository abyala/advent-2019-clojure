(ns advent-2019-clojure.day16-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day16 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest pattern-at-index-test
  (are [index expected] (= expected (take 8 (pattern-at-index index)))
                        0 [1 0 -1 0 1 0 -1 0]
                        2 [0 0 1 1 1 0 0 0]))

(deftest last-digit-test
  (are [n expected] (= expected (last-digit n))
                    0 0
                    5 5
                    -5 5
                    21 1
                    -27 7))

(deftest part1-test
  (are [expected input] (= expected (part1 input))
                        "24176176" "80871224585914546619083218645595"
                        "73745418" "19617804207202209144916044189917"
                        "52432133" "69317163492948606335995924319873"
                        "85726502" puzzle-data))

(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        "84462026" "03036732577212944063491565474664"
                        "78725270" "02935109699940807407585447034323"
                        "53553731" "03081770884921959731165446850517"
                        "92768399" puzzle-data))
