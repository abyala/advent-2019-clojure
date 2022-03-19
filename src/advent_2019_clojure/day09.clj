(ns advent-2019-clojure.day09
  (:require [advent-2019-clojure.intcode :as ic]))

(defn solve [instruction input]
  (let [outputs (-> input ic/parse-input (ic/add-input! instruction) ic/run-to-completion ic/outputs!)]
    (if (= 1 (count outputs))
      (first outputs)
      {:failing-opcodes outputs})))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 2 input))
