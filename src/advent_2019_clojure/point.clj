(ns advent-2019-clojure.point
  (:require [advent-2019-clojure.utils :refer [abs]]))

(def origin [0 0])
(defn manhattan-distance
  ([p] (manhattan-distance origin p))
  ([[x1 y1] [x2 y2]] (+ (abs (- x1 x2))
                        (abs (- y1 y2)))))

(defn move-up [[x y]] [x (inc y)])
(defn move-down [[x y]] [x (dec y)])
(defn move-left [[x y]] [(dec x) y])
(defn move-right [[x y]] [(inc x) y])