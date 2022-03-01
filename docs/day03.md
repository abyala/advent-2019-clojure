# Day 3: Crossed Wires

* [Problem Statement](https://adventofcode.com/2019/day/3)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day03.clj)

---

## Part 1

Today we're taking wiring instructions and need to run calculations on their intersections. Sounds easy enough, so
let's get started.

First, we'll parse the input, which comes in as a comma-separated list of a single character for up, down, left, or 
right, and a number of steps to take. I think the easiest way to read this data is a sequence of 2-element vectors,
composed of the keyword `:up`, `:down`, `:left`, or `:right`, and the number of steps to take. This extra mapping
isn't strictly necessary, but I find it helps to think through the problem statement. Note that for each string `s`,
we use `(rest s)` to grab the first character, and `(subs s 1)` to grab the substring starting with the second
character.

```clojure
(defn parse-directions [input]
  (->> (str/split input #",")
       (map (fn [s] [({\U :up, \D :down, \L :left, \R :right} (first s))
                     (parse-long (subs s 1))]))))
```

Now because each instruction represents a series of steps, let's create a function called `points-from`, which takes
in a starting point, the direction as a keyword, and the number of steps to take. I know from previous years that
Advent problems tends to work with points and grids a lot, so we'll create functions `move-up`, `move-down`,
`move-left`, and `move-right` in the `point` namespace, and map each keyword to its function. Then to implement
`points-from`, we'll pick the correct function and use `iterate` to keep applying it to the current point, keeping
only `distance` number of values. Since the first result is always the initial value passed to `iterate`, we'll call
`rest` first to drop that initial value.

```clojure
; advent-2019-clojure.point namespace
(defn move-up    [point] (update point 1 inc))
(defn move-down  [point] (update point 1 dec))
(defn move-left  [point] (update point 0 dec))
(defn move-right [point] (update point 0 inc))

; advent-2019-clojure.day03 namespace
(def direction-map {:up    point/move-up
                    :down  point/move-down
                    :left  point/move-left
                    :right point/move-right})

(defn points-from [point direction distance]
  (let [f (direction-map direction)]
    (->> (iterate f point)
         rest
         (take distance))))
```

With that, we can implement the full `walk-path` function, which takes in a sequence of parsed directions and maps out
the complete path taken starting from the origin of `[0 0]`. This amounts to calling `reduce` with the list of
directions, but since there can be many steps in the path, my accumulator will be a vector of
`[last-point path-so-far]`; this allow the next `reduce` to immediately start from the final point instead of scanning
each time to the end of the path. Once we have gone through all instructions, we'll use `second` to pull the path out
of the vector to get the list of steps excluding the initial `origin`.

And as I enjoy doing, note the destructuring going on in the reducing function. The function signature is
`(fn [accumulator value])`, but we know that the accumulator is the vector just mentioned, and the value of each
direction is itself a sequence of its keyword direction and the distance. So this ends up destructuring nicely into
`(fn [[point path] [direction distance]])`.

```clojure
; advent-2019-clojure.point namespace
(def origin [0 0])

; advent-2019-clojure.day03 namespace
(defn walk-path [directions]
  (->> directions
       (reduce (fn [[point path] [direction distance]]
                 (let [next-path (points-from point direction distance)]
                   [(last next-path) (apply conj path next-path)]))
               [point/origin []])
       second))
```

Ok, it's time to create the `part1` function. We'll start by splitting the input string into two values based on the
presence of a newline; as we expect two lines of input, these will represent the two wires. Each line will be parsed
into directions, then walked, and then converted into a set because we'll eventually need to see where the two wires
cross. Calling `(apply set/intersection)` will result in those intersecting points, being the ones common to each set.
For each such points, we'll map it to its Manhattan distance, and use `(apply min)` to find the shortest path and our
answer. I'll overload the `manhattan-distance` function in the `point` namespace to support either two points, or a
single point that we'll assume should be compared with the `origin`.

Note here that the use of `(map (comp set walk-path parse-directions))` allows us to avoid the more wordy
`(->> (map parse-directions) (map walk-path) (map set))` by composing the three mapping functions into one.

```clojure
; advent-2019-clojure.point namespace
(defn manhattan-distance
  ([p] (manhattan-distance origin p))
  ([[x1 y1] [x2 y2]] (+ (abs (- x1 x2))
                        (abs (- y1 y2)))))

; advent-2019-clojure.day03 namespace
(defn part1 [input]
  (->> (str/split-lines input)
       (map (comp set walk-path parse-directions))
       (apply set/intersection)
       (map point/manhattan-distance)
       (apply min)))
```

---

## Part 2

In part 2, we need to examine how many steps it takes each wire before it first reaches an intersecting point, and then
select the smallest sum of those distances. This shouldn't be too tough.

First we'll implement `first-time-at-points`, which takes in a sequence of points and returns a mapping of each point
to the number of steps it took to reach it the first time. This function amounts to just one `reduce` call. We'll start
with an empty map and feed the reducing function a sequence of `[p dist]` pairs by calling
`(map list points (rest (range)))`.  `(map list)` will apply the `list` function to the pairs of values from the two 
subsequent collections. `points` is self-explanatory, but we'll use `(rest (range))` to get an infinite sequence that
starts at `1`, since the points will not include the origin. Then the reducing function either returns the accumulated 
map if a given point has already been seen, or else associates the point to its distance.

```clojure
(defn first-time-at-points [points]
  (reduce (fn [acc [p dist]] (if (acc p) acc (assoc acc p dist)))
          {}
          (map list points (rest (range)))))
```

Now we're already prepared to create `part2`. Again we'll split the input, parse each string and create the walking
path, but then we'll convert the two paths into their first-time distance maps. Now we only want to look at the
intersecting points, so I'll do something hopefully clever with the `merge-with` function. This function allows us to
combine two maps, adding the keys of the second to the first, and using the `merge-with` function to combine values if 
both maps contain the same key. In this case, I use `(apply merge-with list)` to swap from a single numeric value to a
list of numeric values; this leaves us with a heterogeneous map with points as keys, and either numbers or lists of 
numbers as values. We'll simplify things again because we want to add the distances for the intersecting points, so the
`keep` function will utilize `(when (list? distances))` to identify the values that are lists instead of numbers, on
which we'll then call `(apply + distances)` to get to the combined distance. From there, it's a simple matter of 
finding the smallest such sum of distances.

```clojure
(defn part2 [input]
  (->> (str/split-lines input)
       (map (comp first-time-at-points walk-path parse-directions))
       (apply merge-with list)
       (keep (fn [[_ distances]] (when (list? distances)
                                   (apply + distances))))
       (apply min)))
```

---

## Refactoring

Can we combine parts 1 and 2 into a common function? Yes, I think we can!

So let's think about what's common here. In both cases, we need to parse the inputs and do something with the
intersecting points. In part 1, we just find the smallest Manhattan distance for each point, while in part 2 we only
need to find the shortest combined distance to that point. So we're running a different function for each part, and
returning the smallest value.

We'll use a combined `solve` function that leverages the `first-time-at-points` function and the `(apply merge-with
list)` logic introduced for part 2. We'll again use `list?` to only examine intersections, but this time we'll use a
`filter` and `map` instead of a `keep` to simplify the code. Once we have only the intersections, which at this point
is a sequence of `[point [distances]]`, we use different mapping functions. `part1` will use 
`(comp point/manhattan-distance first)` to drop the distances and calculate the Manhattan distance for the point.
`part2` will use `(comp (partial apply +) second)` to drop the point and add together the distances. Then `solve` finds
the minimum value, and we have a combined, arguably simplified solution.

```clojure
(defn solve [f input]
  (->> (str/split-lines input)
       (map (comp first-time-at-points walk-path parse-directions))
       (apply merge-with list)
       (filter #(list? (second %)))
       (map f)
       (apply min)))

(def part1 (partial solve (comp point/manhattan-distance first)))
(def part2 (partial solve (comp (partial apply +) second)))
```