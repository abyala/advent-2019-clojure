# Day 10: Monitoring Station

* [Problem Statement](https://adventofcode.com/2019/day/10)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day10.clj)

---

## Part 1

Today we're measuring asteroids' abilities to detect other asteroids within a 2-dimensional grid. In the first part,
we receive an input map and need to find which asteroid has the most distinct slopes between it and its peers.

To start with parsing, we'll read through the input data and return the set of `[x y]` coordinates that correspond to
asteroids. There's no need to keep the structure of the map itself, since the asteroids won't be moving. I've added
the `parse-to-char-coords` function in the `advent-2019-clojure.utils` package that I've used in previous years, and
then again use `transduce` to keep only the `coordinates` of the asteroids, conjoining them into an empty set. And
while not strictly needed, the `asteroid?` helper function makes the `filter` look more intuitive to me.

```clojure
(defn asteroid? [c] (= c \#))
(defn parse-asteroids [input]
  (transduce (comp (filter #(asteroid? (second %)))
                   (map first))
             conj
             #{}
             (point/parse-to-char-coords input)))
```

Now we want to make a function called `lines-of-sight`, which takes in the point of a single asteroid and a collection
of its peers, and return a map of `{slope [ordered-peers]}`, such that two asteroids `a` and `b` with the same slope `s`
relative to the source would show up as `{slope [a b]}`. Originally I tried to use `(y1-y0)/(x1-x0)` or "rise over run"
to calculate the slope, but it did not handle the infinite values of two vertical points, nor distinguished between
points to the left nor the right. So instead, I checked Google for how to express a slope in terms of Radians (it's
been decades since I've done trigonometry!), and learned that the function `Math/atan2` would calculate this. This
became my `slope` function.

```clojure
(defn slope [[x0 y0] [x1 y1]]
  (Math/atan2 (- y1 y0) (- x1 x0)))
```

From there, I could implement `lines-of-sight` by reducing all the source's peer points into a map, keyed by the slope
between each peer and the source. My utility function `map-conj` conveniently adds each value into a collection, thus
eliminating the need to code both an `assoc` and `update` based on if the value was already present.

```clojure
(defn lines-of-sight [source points]
  (reduce (fn [m p] (utils/map-conj m (slope source p) p))
          {}
          points))
```

Now that we can create this map for a point for each of its slopes, we can implement `lines-of-sight-map`, which maps
each point to its map of `lines-of-sight`. Thus we'll be able to do the calculation for all asteroids.

```clojure
(defn lines-of-sight-map [asteroids]
  (->> asteroids
       (map (fn [ast] [ast (lines-of-sight ast (disj asteroids ast))]))
       (into {})))
```

Finally, we can implement the `part1` function. We'll parse the asteroids, convert them into the `lines-of-sight-map`,
map each asteroid to the number of distinct slopes, and return the largest value.

```clojure
(defn part1 [input]
  (->> (parse-asteroids input)
       lines-of-sight-map
       (map (comp count second))
       (reduce max)))
```

---

## Part 2

This part is a bit trickier, because we need to look at each slope in a particular order, namely starting from due
North and going clockwise. The problem is that zero radians is due East, and radian values go counter-clockwise, so
our `slope` function isn't quite good enough. Not only that, but when Java provides values like `3*Pi/2` radians, it
does so using the mathematically equivalent value of `-Pi/2` radians.

I created a function `clock-radians` based entirely on trial and error of messing around with different inputs, such
that it returns values from `0` to `2*Pi` in a clockwise fashion. I'm not sure if it makes any mathematical sense at
all, but it's a thing now and the universe will adjust.

```clojure
(def ^:private two-pi (* Math/PI 2))
(defn clock-radians [[x0 y0] [x1 y1]]
  (mod (Math/atan2 (- x1 x0) (- y0 y1))
       two-pi))
```

With that, we can make two changes to the `lines-of-sight` function. First, in stead of using the `slope` from the
source to each peer coordinates, we'll use the `clock-radians` function instead. Second, instead of using the simple
`map-conj` function, we now need to create a sorted set based on the distance from each point on a slope to the source,
and for that our comparator function will calculate the Manhattan Distance. I decided to enhance the `map-conj`
function to take in an optional supplier function whenever adding a new key into the map, and a `map-conj-sorted`
function that takes in a comparator and creates the sorted set automatically. This kept the logic of the
`lines-of-sight` still focused on the business goal and not the mechanics.

```clojure
; advent-2019-clojure.utils namespace
(defn map-conj
  ([m k v] (map-conj #(vector %) m k v))
  ([collection-supplier m k v] (if (m k)
                                 (update m k conj v)
                                 (assoc m k (collection-supplier v)))))

(defn map-conj-sorted [comparator m k v]
  (map-conj (partial sorted-set-by comparator) m k v))

; advent-2019-clojure.day10 namespace
(defn lines-of-sight [source points]
  (let [comparator (fn [p1 p2] (apply compare (map (partial point/manhattan-distance source)
                                                   [p1 p2])))]
    (reduce (fn [m p] (utils/map-conj-sorted comparator m (clock-radians source p) p))
            {}
            points)))
```

Now we need a function, `best-station`, that takes in the `lines-of-sight-map` and returns the map of slopes to points
for whichever station has the most distinct slopes. Here we'll sort each map by the count of map values, take the last
(largest) value, and just return its map; `first` is the point's coordinates and `second` is its data map. Note that
we can now refactor the `part1` function to leverage the `best-station` function.

```clojure
(defn best-station [lines-of-sight]
  (->> lines-of-sight
       (sort-by (comp count second))
       last
       second))

(defn part1 [input]
  (->> (parse-asteroids input)
       lines-of-sight-map
       best-station
       count))
```

In order for us to create the `part2` function, we'll need to make one more helper function, called `interleave-all`.
The `clojure.core/interleave` function only works well for equal-length collections. In our case, we want to interleave
every value from every collection, instead of giving up when the first collection is empty. This turned out to be
rather straightforward. We construct a lazy collection of the first values in each input collection, and then use
`lazy-seq` to concatenate them to the `rest` of every collection, removing the ones that are empty.

```clojure
; advent-2019-clojure.utils namespace
(defn interleave-all [colls]
  (when (seq colls)
    (lazy-seq (concat (map first colls)
                      (interleave-all (->> colls (map rest) (filter seq)))))))
```

Ok, we're ready to write `part2`. First off, we'll create the `vaporize-seq` of asteroids to be destroyed in order,
starting from the best station. Once we call `best-station`, we'll sort the slope map by the clock radian slopes, 
then map each value two its sorted map of points along each slope, and call `interleave-all` to sequence them properly.
Then once we find the 200th asteroid, in position 199, we run the mathematical calculation on the `x` and `y` 
coordinates to come up with the right answer.

```clojure
(defn part2 [input]
  (let [vaporize-seq (->> (parse-asteroids input)
                          lines-of-sight-map
                          best-station
                          (sort-by first)
                          (map second)
                          (utils/interleave-all))
        [target-x target-y] (nth vaporize-seq 199)]
    (+ (* target-x 100) target-y)))
```
