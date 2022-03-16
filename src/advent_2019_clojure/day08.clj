(ns advent-2019-clojure.day08)

(def width 25)
(def height 6)

(defn parse-layers [input]
  (partition (* width height) input))

(defn part1 [input]
  (->> (parse-layers input)
       (map frequencies)
       (sort-by #(get % \0 0) <)
       (map #(* (get % \1 0) (get % \2 0)))
       first))

(defn visible-pixel [pixels]
  (first (keep {\0 \space \1 \#} pixels)))

(defn part2 [input]
  (let [layers (parse-layers input)]
    (->> (apply interleave layers)
         (partition (count layers))
         (map visible-pixel)
         (partition width)
         (map (partial apply str))
         (run! println))))
