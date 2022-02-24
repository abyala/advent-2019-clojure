(ns advent-2019-clojure.day02
  (:require
    [advent-2019-clojure.utils :refer [parse-long]]
    [clojure.string :as str]))

(defn parse-input [input] {:instruction-pointer 0
                           :addresses           (mapv parse-long (str/split input #","))})

(def instruction-length 4)
(defn current-op [{:keys [instruction-pointer addresses]}]
  (get addresses instruction-pointer))
(defn current-instruction [{:keys [instruction-pointer addresses]}]
  (subvec addresses instruction-pointer (+ instruction-pointer instruction-length)))

(defn address-value [int-code address]
  (get-in int-code [:addresses address]))
(defn set-address-value [int-code address v]
  (assoc-in int-code [:addresses address] v))
(defn advance-instruction-pointer [int-code]
  (update int-code :instruction-pointer + instruction-length))

(defn- arithmatic-instruction [f int-code]
  (let [[_ a b c] (current-instruction int-code)]
    (-> int-code
        (set-address-value c (f (address-value int-code a)
                                (address-value int-code b)))
        (advance-instruction-pointer))))
(defmulti run-instruction current-op)
(defmethod run-instruction 1 [int-code] (arithmatic-instruction + int-code))
(defmethod run-instruction 2 [int-code] (arithmatic-instruction * int-code))
(defmethod run-instruction 99 [_] nil)

(defn run-to-completion [int-code]
  (->> (iterate run-instruction int-code)
       (take-while some?)
       last))

(defn output [int-code noun verb]
  (-> int-code
      (set-address-value 1 noun)
      (set-address-value 2 verb)
      (run-to-completion)
      (address-value 0)))

(defn part1 [input] (-> input parse-input (output 12 2)))
(defn part2 [input]
  (let [int-code (parse-input input)]
    (first (for [noun (range 1 100)
                 verb (range 1 100)
                 :when (= (output int-code noun verb) 19690720)]
             (-> 100 (* noun) (+ verb))))))
