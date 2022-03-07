(ns advent-2019-clojure.day07
  (:require
    [advent-2019-clojure.intcode :as ic]
    [clojure.math.combinatorics :as combo]))

(defn run-amplifier-series [input phases]
  (first (reduce (fn [previous-outputs phase-setting]
             (-> (ic/parse-input input (cons phase-setting previous-outputs))
                 (ic/run-to-completion)
                 (ic/outputs)))
           [0]
           phases)))

(defn part1 [input]
  (->> (combo/permutations (range 5))
       (map (partial run-amplifier-series input))
       (apply max)))
