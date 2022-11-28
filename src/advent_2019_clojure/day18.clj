(ns advent-2019-clojure.day18
  (:require [advent-2019-clojure.point :as point]
            [clojure.set :as set]))

; TODO: Consider making walkable? include an unlockable door

(def wall-char \#)
(def space-char \.)
(def player-char \@)
(defn wall? [c] (= c wall-char))
(defn player? [c] (= c player-char))
(defn space? [c] (= c space-char))
(defn door? [^Character c] (Character/isUpperCase c))
(defn point-at [maze p] (get-in maze [:points p]))
(defn key? [^Character c] (Character/isLowerCase c))
(defn key?
  ([^Character c] (Character/isLowerCase c))
  ([maze p] (key? (point-at maze p))))
(defn walkable?
  ([c] (#{space-char player-char} c))
  ([maze p] (walkable? (point-at maze p))))
(defn player-point [maze] (:player maze))
(def in-maze? point-at)
(defn set-point-at [maze point c]
  (assoc-in maze [:points point] c))
(defn unlocked? [maze ^Character door-name]
  (get-in maze [:keys (Character/toLowerCase door-name)]))
(defn locked-door? [maze p]
  (let [c (point-at maze p)]
    (and (door? c)
         (not (unlocked? maze c)))))
(defn unlockable-door? [maze p]
  (let [c (point-at maze p)]
    (and (door? c)
         (unlocked? maze c))))

(defn move-player-to [maze point]
  (cond
    (or (walkable? maze point) (unlockable-door? maze point))
    (-> maze
        (set-point-at (player-point maze) space-char)
        (set-point-at point player-char)
        (assoc :player point))

    (key? maze point)
    (-> maze
        (set-point-at (player-point maze) space-char)
        (set-point-at point player-char)
        (update :keys conj (point-at maze point))
        (assoc :player point))

    :else
    nil))

(defn parse-maze [input]
  (let [points (->> (point/parse-to-char-coords input)
                    (remove #(wall? (second %)))
                    (into {}))
        player (->> points (filter #(player? (second %))) ffirst)]
    {:points points
     :player player
     :keys   #{}}))

(defn adjacent-moves [maze seen]
  (->> (point/adjacent-points (player-point maze))
       (filter (partial in-maze? maze))
       (remove seen)
       seq))

(defn all-moves [maze]
  (let [maze-comp (fn [x y] (compare [(:steps x) (:point x)] [(:steps y) (:point y)]))]
    (loop [options (sorted-set-by maze-comp {:point (player-point maze) :steps 0}) seen {} stops {}]
      (if-some [{:keys [point steps] :as option} (first options)]
        (let [maze' (move-player-to maze point)
              options' (disj options option)
              seen' (assoc seen point steps)]
          (cond
            (locked-door? maze point)
            (recur options' seen' stops)

            (key? maze point)
            (recur options' seen' (assoc stops point steps))

            (or (walkable? maze point)
                (unlockable-door? maze point))
            (recur (apply conj options' (map #(hash-map :point % :steps (inc steps))
                                             (adjacent-moves maze' seen')))
                   seen'
                   stops)

            :else
            (recur options' seen' stops)))
        stops))))

(defn finished? [maze]
  (not-any? key? (-> maze :points vals)))

(defn min-steps-to-all-keys [initial-maze]
  (loop [maze-distances {initial-maze 0}]
    (when-let [[dist maze] (-> maze-distances set/map-invert sort first)]
      (if (finished? maze)
        dist
        (let [all-destinations-from-here (all-moves maze)
              new-dest-map (into {} (map (fn [[point steps]] [(move-player-to maze point) (+ dist steps)])
                                         all-destinations-from-here))]
          (recur (merge-with min (dissoc maze-distances maze) new-dest-map)))))))

(defn part1 [input]
  (-> input parse-maze min-steps-to-all-keys))
