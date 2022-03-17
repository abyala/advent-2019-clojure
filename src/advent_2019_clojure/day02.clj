(ns advent-2019-clojure.day02
  (:require [advent-2019-clojure.intcode :as ic]))

(defn output [int-code noun verb]
  (-> int-code
      (ic/set-address-value 1 noun)
      (ic/set-address-value 2 verb)
      (ic/run-to-completion)
      (ic/address-value 0)))

(defn part1 [input] (-> input ic/parse-input (output 12 2)))
(defn part2 [input]
  (let [int-code (ic/parse-input input)]
    (first (for [noun (range 1 100)
                 verb (range 1 100)
                 :when (= (output int-code noun verb) 19690720)]
             (-> 100 (* noun) (+ verb))))))
