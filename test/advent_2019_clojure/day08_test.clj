(ns advent-2019-clojure.day08-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day08 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (= 2562 (part1 puzzle-data)))

; There is no meaningful test case for part 2.  Just run it.
; The output will be the string "ZFLBY" displayed on the console.

(part2 puzzle-data)
