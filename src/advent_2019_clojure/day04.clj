(ns advent-2019-clojure.day04)

(defn solve [adjacency-fn low high]
  (->> (range low (inc high))
       (map str)
       (filter (fn [s] (and (= 6 (count s))
                            (some adjacency-fn (partition-by identity s))
                            (->> (map int s)
                                 (partition 2 1)
                                 (not-any? (partial apply >))))))
       count))

(def part1 (partial solve #(> (count %) 1)))
(def part2 (partial solve #(= (count %) 2)))