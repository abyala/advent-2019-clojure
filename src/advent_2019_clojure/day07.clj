(ns advent-2019-clojure.day07
  (:require
    [advent-2019-clojure.intcode :as ic]
    [clojure.math.combinatorics :as combo]))

(defn run-amplifier-loop [input phases]
  (let [int-codes (mapv (fn [phase] (-> (ic/parse-input input)
                                        (ic/add-input! phase)))
                       phases)]
    (ic/add-input! (first int-codes) 0)
    (run! (fn [[outputter inputter]] (ic/chain-outputs-to! outputter inputter))
          (partition 2 1 (conj int-codes (first int-codes))))
    (run! #(future (ic/run-to-completion %))
          int-codes)
    (->> int-codes last ic/outputs! last)))

(defn solve [possible-phases input]
  (->> (combo/permutations possible-phases)
       (map (partial run-amplifier-loop input))
       (apply max)))

(defn part1 [input] (solve (range 5) input))
(defn part2 [input] (solve (range 5 10) input))
