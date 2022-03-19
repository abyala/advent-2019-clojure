(ns advent-2019-clojure.intcode
  (:require
    [advent-2019-clojure.utils :refer [parse-long]]
    [clojure.core.async :as async :refer [>!! <!! <! go chan mult tap close!]]
    [clojure.string :as str]))

(defn parse-input [s]
  (let [out-chan (chan)
        out-mult (mult out-chan)
        out-copy-chan (chan 1000)]
    (tap out-mult out-copy-chan)
    {:instruction-pointer 0
     :relative-base       0
     :addresses           (reduce-kv (fn [m k v] (assoc m k (parse-long v))) {} (str/split s #","))
     :input               (chan 1000)
     :output              {:channel   out-chan
                           :mult      out-mult
                           :copy-chan out-copy-chan}}))

(defn current-op [{:keys [instruction-pointer addresses]}]
  (-> addresses
      (get instruction-pointer)
      (mod 100)))

(def ^:private mode-types {\0 :position, \1 :immediate, \2 :relative})
(defn current-params [{:keys [instruction-pointer addresses]} num-params]
  (let [params (->> (select-keys addresses (range (inc instruction-pointer) (+ instruction-pointer num-params 1)))
                    (sort-by first)
                    (mapv second))
        modes (->> (get addresses instruction-pointer) str reverse (drop 2) vec)]
    (mapv (fn [n] {:mode  (mode-types (get modes n \0))
                   :value (params n)})
          (range num-params))))

(defn address-value [int-code address]
  (get-in int-code [:addresses address] 0))
(defn set-address-value [int-code address v]
  (assoc-in int-code [:addresses address] v))
(defn advance-instruction-pointer [int-code distance]
  (update int-code :instruction-pointer + distance))
(defn set-instruction-pointer [int-code value]
  (assoc int-code :instruction-pointer value))
(defn relative-base [int-code]
  (:relative-base int-code))
(defn param-value [int-code {:keys [mode value]}]
  (case mode
    :position (address-value int-code value)
    :immediate value
    :relative (address-value int-code (+ value (relative-base int-code)))))
(defn param-address-value [int-code {:keys [mode value]}]
  (if (= :relative mode)
    (+ (relative-base int-code) value)
    value))

(defn add-input! [int-code v]
  (>!! (:input int-code) v)
  int-code)
(defn outputs! [int-code]
  (<!! (async/into [] (get-in int-code [:output :copy-chan]))))
(defn add-output! [int-code v]
  (>!! (get-in int-code [:output :channel]) v)
  int-code)
(defn chain-outputs-to! [output-int-code input-int-code]
  (tap (get-in output-int-code [:output :mult]) (:input input-int-code)))

(defn- run-and-advance [f num-params int-code]
  (-> (f int-code (current-params int-code num-params))
      (advance-instruction-pointer (inc num-params))))

(defn- arithmetic-instruction [f int-code params]
  (let [[a b c] params
        target-address (param-address-value int-code c)]
    (set-address-value int-code target-address (f (param-value int-code a)
                                         (param-value int-code b)))))
(defn- input-instruction! [int-code [param]]
  (let [input (<!! (:input int-code))
        target-address (param-address-value int-code param)]
    (set-address-value int-code target-address input)))
(defn- output-instruction! [int-code [param]]
  (let [output (param-value int-code param)]
    (add-output! int-code output)))
(defn- jump-if-instruction [pred int-code]
  (let [[a b] (current-params int-code 2)]
    (if (pred (param-value int-code a))
      (set-instruction-pointer int-code (param-value int-code b))
      (advance-instruction-pointer int-code 3))))
(defn- compare-instruction [pred int-code params]
  (let [[a b c] params
        target-address (param-address-value int-code c)]
    (set-address-value int-code target-address (if (pred (param-value int-code a)
                                                         (param-value int-code b))
                                                 1 0))))
(defn- adjust-relative-base [int-code params]
  (let [adjustment (param-value int-code (first params))]
    (update int-code :relative-base + adjustment)))

(defn- halt-instruction [int-code]
  (go (close! (:input int-code)))
  (go (close! (get-in int-code [:output :channel])))
  (assoc int-code :halted? true))

(defmulti run-instruction current-op)
(defmethod run-instruction 1 [int-code] (run-and-advance (partial arithmetic-instruction +) 3 int-code))
(defmethod run-instruction 2 [int-code] (run-and-advance (partial arithmetic-instruction *) 3 int-code))
(defmethod run-instruction 3 [int-code] (run-and-advance input-instruction! 1 int-code))
(defmethod run-instruction 4 [int-code] (run-and-advance output-instruction! 1 int-code))
(defmethod run-instruction 5 [int-code] (jump-if-instruction (complement zero?) int-code))
(defmethod run-instruction 6 [int-code] (jump-if-instruction zero? int-code))
(defmethod run-instruction 7 [int-code] (run-and-advance (partial compare-instruction <) 3 int-code))
(defmethod run-instruction 8 [int-code] (run-and-advance (partial compare-instruction =) 3 int-code))
(defmethod run-instruction 9 [int-code] (run-and-advance adjust-relative-base 1 int-code))
(defmethod run-instruction 99 [int-code] (halt-instruction int-code))

(defn run-to-completion [int-code]
  (->> (iterate run-instruction int-code)
       (filter :halted?)
       first))
