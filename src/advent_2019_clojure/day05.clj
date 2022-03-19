(ns advent-2019-clojure.day05
  (:require [advent-2019-clojure.intcode :as ic]))

(defn program-outputs [program single-input]
  (-> (ic/parse-input program)
      (ic/add-input! single-input)
      (ic/run-to-completion)
      (ic/outputs!)))

(defn part1 [input]
  (let [output (program-outputs input 1)]
    (if (every? zero? (butlast output))
      (last output)
      {:diagnostic-error (butlast output)})))

(defn part2 [input]
  (first (program-outputs input 5)))
