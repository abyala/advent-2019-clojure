(ns advent-2019-clojure.intcode-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.intcode :refer :all]))

(defn address-after-one-instruction [input address]
  (-> input parse-input run-instruction (address-value address)))

(deftest add-test
  (testing "Position mode"
    (are [input address expected] (= expected (address-after-one-instruction input address))
                                  "1,2,4,0,99" 0 103
                                  "1,2,4,0,99" 3 0
                                  "1,2,4,3,99" 0 1
                                  "1,2,4,3,99" 3 103))
  (testing "Immediate mode"
    (are [input address expected] (= expected (address-after-one-instruction input address))
                                  "1,2,3,0,99" 0 3
                                  "101,2,3,0,99" 0 2
                                  "1001,2,3,0,99" 0 6
                                  "1101,2,3,0,99" 0 5)))

(deftest multiply-test
  (testing "Position mode"
    (are [input address expected] (= expected (address-after-one-instruction input address))
                                  "2,2,4,0,99" 0 396
                                  "2,2,4,0,99" 3 0
                                  "2,2,4,3,99" 0 2
                                  "2,2,4,3,99" 3 396))
  (testing "Immediate mode"
    (are [input address expected] (= expected (address-after-one-instruction input address))
                                  "2,2,4,0,99" 0 396
                                  "102,2,4,0,99" 0 198
                                  "1002,2,4,0,99" 0 16
                                  "1102,2,4,0,99" 0 8)))

(deftest load-input-test
  (let [int-code (run-instruction (parse-input "3,50" [456]))]
    (is (= 2 (:instruction-pointer int-code)))
    (is (empty? (inputs int-code)))
    (is (= 456 (address-value int-code 50)))
    (is (nil? (address-value int-code 500)))))

(deftest output-test
  (let [int-code (-> (parse-input "3,40,4,40" [456]) run-instruction run-instruction)]
    (is (= 4 (:instruction-pointer int-code)))
    (is (= [456] (:outputs int-code)))))

(deftest jump-if-true-test
  (testing "Position mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (-> input parse-input run-instruction :instruction-pointer)))
                                              "5,0,4,6,7,0" 7
                                              "5,5,4,6,7,0" 3))
  (testing "Immediate mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (-> input parse-input run-instruction :instruction-pointer)))
                                              "105,0,4,6,7,0" 3
                                              "1005,0,4,6,7,0" 4
                                              "1105,0,4,6,7,0" 3
                                              "105,5,4,6,7,0" 7
                                              "1005,5,4,6,7,0" 3
                                              "1105,5,4,6,7,0" 4)))

(deftest jump-if-false-test
  (testing "Position mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (-> input parse-input run-instruction :instruction-pointer)))
                                              "6,0,4,6,7,0" 3
                                              "6,5,4,6,7,0" 7))
  (testing "Immediate mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (-> input parse-input run-instruction :instruction-pointer)))
                                              "106,0,4,6,7,0" 7
                                              "1006,0,4,6,7,0" 3
                                              "1106,0,4,6,7,0" 4
                                              "106,5,4,6,7,0" 3
                                              "1006,5,4,6,7,0" 4
                                              "1106,5,4,6,7,0" 3)))

(deftest less-than-test
  (testing "Position mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-one-instruction input 5)))
                                              "7,3,0,5,3,8" 1
                                              "7,3,3,5,3,8" 0
                                              "7,3,1,5,3,8" 0))
  (testing "Immediate mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-one-instruction input 5)))
                                              "107,3,0,5,3,8" 1
                                              "1007,3,0,5,3,8" 0
                                              "1107,3,0,5,3,8" 0
                                              "107,3,5,5,3,8" 1
                                              "1007,3,5,5,3,8" 0
                                              "1107,3,5,5,3,8" 1)))

(deftest equals-test
  (testing "Position mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-one-instruction input 5)))
                                              "8,3,0,5,3,8" 0
                                              "8,3,3,5,3,8" 1
                                              "8,3,1,5,3,8" 0))
  (testing "Immediate mode"
    (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-one-instruction input 5)))
                                              "108,3,0,5,3,8" 0
                                              "1008,3,0,5,3,8" 0
                                              "1108,3,0,5,3,8" 0
                                              "108,3,5,5,3,8" 0
                                              "1008,3,5,5,3,8" 1
                                              "1108,3,5,5,3,8" 0)))

(deftest day5-part2-sample-data
  (let [program "3,21,1008,21,8,20,1005,20,22,107,8,21,20,1006,20,31,1106,0,36,98,0,0,1002,21,125,20,4,20,1105,1,46,104,999,1105,1,46,1101,1000,1,20,4,20,1105,1,46,98,99"]
    (are [expected input] (is (= expected (-> (parse-input program [input]) run-to-completion :outputs first)))
                          999 7
                          1000 8
                          1001 15)))