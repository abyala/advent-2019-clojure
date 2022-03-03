(ns advent-2019-clojure.intcode
  (:require
    [advent-2019-clojure.utils :refer [parse-long]]
    [clojure.string :as str]))

(defn parse-input
  ([s] (parse-input s []))
  ([s inputs] {:instruction-pointer 0
               :addresses           (reduce-kv (fn [m k v] (assoc m k (parse-long v))) {} (str/split s #","))
               :inputs              inputs
               :outputs             []}))

(defn current-op [{:keys [instruction-pointer addresses]}]
  (-> addresses
      (get instruction-pointer)
      (mod 100)))

(def ^:private mode-types {\0 :position, \1 :immediate})
(defn current-params [{:keys [instruction-pointer addresses]} num-params]
  (let [params (->> (select-keys addresses (range (inc instruction-pointer) (+ instruction-pointer num-params 1)))
                    (sort-by first)
                    (mapv second))
        modes (->> (get addresses instruction-pointer) str reverse (drop 2) vec)]
    (mapv (fn [n] {:mode  (mode-types (get modes n \0))
                   :value (params n)})
          (range num-params))))

(defn address-value [int-code address]
  (get-in int-code [:addresses address]))
(defn set-address-value [int-code address v]
  (assoc-in int-code [:addresses address] v))
(defn advance-instruction-pointer [int-code distance]
  (update int-code :instruction-pointer + distance))
(defn set-instruction-pointer [int-code value]
  (assoc int-code :instruction-pointer value))
(defn param-value [int-code {:keys [mode value]}]
  (case mode
    :position (address-value int-code value)
    :immediate value))
(def param-immediate-value :value)

(defn inputs [int-code]
  (:inputs int-code))
(defn drop-inputs
  ([int-code] (drop-inputs int-code 1))
  ([int-code n] (update int-code :inputs (partial drop n))))
(defn outputs [int-code]
  (:outputs int-code))
(defn add-output [int-code v]
  (update int-code :outputs conj v))

(defn- arithmetic-instruction [f int-code]
  (let [[a b c] (current-params int-code 3)]
    (-> int-code
        (set-address-value (param-immediate-value c) (f (param-value int-code a)
                                                        (param-value int-code b)))
        (advance-instruction-pointer 4))))
(defn- input-instruction [int-code]
  (let [[input] (inputs int-code)
        address (-> int-code (current-params 1) first param-immediate-value)]
    (-> int-code
        (set-address-value address input)
        (drop-inputs)
        (advance-instruction-pointer 2))))
(defn- output-instruction [int-code]
  (let [output (param-value int-code (-> int-code (current-params 1) first))]
    (-> int-code
        (add-output output)
        (advance-instruction-pointer 2))))
(defn- jump-if-instruction [pred int-code]
  (let [[a b] (current-params int-code 2)]
    (if (pred (param-value int-code a))
      (set-instruction-pointer int-code (param-value int-code b))
      (advance-instruction-pointer int-code 3))))
(defn- compare-instruction [pred int-code]
  (let [[a b c] (current-params int-code 3)]
    (-> int-code
        (set-address-value (param-immediate-value c) (if (pred (param-value int-code a)
                                                               (param-value int-code b))
                                                       1 0))
        (advance-instruction-pointer 4))))

(defmulti run-instruction current-op)
(defmethod run-instruction 1 [int-code] (arithmetic-instruction + int-code))
(defmethod run-instruction 2 [int-code] (arithmetic-instruction * int-code))
(defmethod run-instruction 3 [int-code] (input-instruction int-code))
(defmethod run-instruction 4 [int-code] (output-instruction int-code))
(defmethod run-instruction 5 [int-code] (jump-if-instruction (complement zero?) int-code))
(defmethod run-instruction 6 [int-code] (jump-if-instruction zero? int-code))
(defmethod run-instruction 7 [int-code] (compare-instruction < int-code))
(defmethod run-instruction 8 [int-code] (compare-instruction = int-code))
(defmethod run-instruction 99 [_] nil)

(defn run-to-completion [int-code]
  (->> (iterate run-instruction int-code)
       (take-while some?)
       last))
