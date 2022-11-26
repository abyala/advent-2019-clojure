(ns advent-2019-clojure.day16
  (:require [advent-2019-clojure.utils :refer [abs parse-long]]))

(def base-pattern [0 1 0 -1])

(defn pattern-at-index [index]
  (->> (cycle base-pattern)
       (mapcat #(repeat (inc index) %))
       rest))

(defn last-digit [n]
  (abs (rem n 10)))

(defn numeric-array [s]
  (map #(Character/getNumericValue ^char %) s))

(defn calc-signal
  ([digits]
   (map #(calc-signal digits %) (range (count digits))))

  ([digits index]
   (->> (pattern-at-index index)
        (map * digits)
        (apply +)
        last-digit)))

(defn calc-signal-extended [digits]
  (->> (reduce (fn [[acc sum] idx]
                 (let [sum' (last-digit (+ sum (digits idx)))]
                   [(conj acc sum') sum']))
               [() 0]
               (range (dec (count digits)) -1 -1))
       first
       vec))

(defn message-offset [digits]
  (->> (take 7 digits)
       (apply str)
       parse-long))

(defn repeat-signal [digits]
  (->> (repeat 10000 digits)
       (apply concat)
       (drop (message-offset digits))
       vec))

(defn solve [input-fn iterate-fn input]
  (->> (iterate iterate-fn (input-fn (numeric-array input)))
       (drop 100)
       first
       (take 8)
       (apply str)))

(def part1 (partial solve identity calc-signal))
(def part2 (partial solve repeat-signal calc-signal-extended))