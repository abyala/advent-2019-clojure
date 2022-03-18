(ns advent-2019-clojure.intcode-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [advent-2019-clojure.intcode :refer :all]))

(defn address-after-one-instruction [input address]
  (-> input parse-input run-instruction (address-value address)))
(defn address-after-all-instructions [input address]
  (-> input parse-input run-to-completion (address-value address)))
(defn outputs-after-all-instructions [input]
  (-> input parse-input run-to-completion outputs))

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
                                  "1101,2,3,0,99" 0 5))
  (testing "Relative mode"
    (testing "Relative input parameters"
      (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-all-instructions input 50)))
                                                "109,2,1201,2,3,50,99" 6
                                                "109,2,2201,2,3,50,99" 53
                                                "109,2,2101,2,3,50,99" 52))
    (testing "Relative output parameter"
      (are [input address expected] (is (= expected (address-after-all-instructions input address)))
                                    "109,2,21101,2,3,50,99", 50, 0
                                    "109,2,21101,2,3,50,99", 52, 5))))

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
  (testing "Position mode"
    (let [int-code (-> (parse-input "3,50") (add-input 456) run-instruction)]
      (is (= 2 (:instruction-pointer int-code)))
      (is (= 456 (address-value int-code 50)))
      (is (zero? (address-value int-code 500)))))
  (testing "Relative mode"
    (let [int-code (-> (parse-input "109,1,203,2,99") (add-input 456) run-to-completion)]
      (is (= 203 (address-value int-code 2)))
      (is (= 456 (address-value int-code 3))))))

(deftest output-test
  (let [int-code (-> (parse-input "3,40,4,40,99") (add-input 456) run-instruction run-instruction run-instruction)]
    (is (= 4 (:instruction-pointer int-code)))
    (is (= [456] (outputs int-code)))))

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
                                              "1107,3,5,5,3,8" 1))
  (testing "Relative mode"
    (testing "Relative input parameters"
      (are [input expected-instruction-pointer] (is (= expected-instruction-pointer (address-after-all-instructions input 5)))
                                                "109,1,2107,3,0,5,99" 0
                                                "109,1,2207,3,0,5,99" 1
                                                "109,1,1207,3,0,5,99" 0))
    (testing "Relative output parameter"
      (are [input address expected] (is (= expected (address-after-all-instructions input address)))
                                    "109,1,1107,3,5,2,99" 2 1
                                    "109,1,1107,6,5,2,99" 2 0
                                    "109,1,21107,3,5,2,99" 2 21107
                                    "109,1,21107,3,5,2,99" 3 1
                                    "109,1,21107,6,5,2,99" 2 21107
                                    "109,1,21107,6,5,2,99" 3 0))))

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

(deftest adjust-relative-base-test
  (testing "Base case"
    (is (zero? (-> "99" parse-input run-to-completion :relative-base))))
  (testing "Position mode"
    (are [input expected] (= expected (-> input parse-input run-to-completion :relative-base))
                          "9,0,99" 9
                          "9,0,9,4,99" 108))
  (testing "Immediate mode"
    (are [input expected] (= expected (-> input parse-input run-to-completion :relative-base))
                          "109,15,99" 15
                          "109,15,109,4,99" 19)))

(deftest relative-mode-test
  (testing "Addition"
    (are [input address expected] (= expected (address-after-all-instructions input address))
                                  "109,3,1201,1,2,50,99" 50 4
                                  "109,3,2101,1,2,50,99" 50 51
                                  "109,3,2201,1,2,50,99" 50 52)))

(deftest day9-test-data
  (testing "Outputs the program itself"
    (let [input "109,1,204,-1,1001,100,1,100,1008,100,16,101,1006,101,0,99"]
      (is (= input (->> input outputs-after-all-instructions (str/join ","))))))
  (testing "16-digit output"
    (is (= 16 (-> "1102,34915192,34915192,7,4,7,99,0" outputs-after-all-instructions first str count))))
  (testing "Large number in the middle"
    (is (= 1125899906842624 (-> "104,1125899906842624,99" outputs-after-all-instructions first)))))