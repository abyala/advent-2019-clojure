(ns advent-2019-clojure.point
  (:require [advent-2019-clojure.utils :refer [abs]]))

(def origin [0 0])
(defn manhattan-distance
  ([p] (manhattan-distance origin p))
  ([[x1 y1] [x2 y2]] (+ (abs (- x1 x2))
                        (abs (- y1 y2)))))

(defn move-up [point] (update point 1 inc))
(defn move-down [point] (update point 1 dec))
(defn move-left [point] (update point 0 dec))
(defn move-right [point] (update point 0 inc))