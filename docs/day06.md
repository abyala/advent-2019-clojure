# Day 6: Universal Orbit Map

* [Problem Statement](https://adventofcode.com/2019/day/6)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day06.clj)

---

## Part 1

Today we're playing with planetary orbits!  We're given a list of which objects orbit which other ones, and from this
we need to plot some paths.

To start, let's parse all the orbits. An object can be orbited by multiple other objects, but I found it easier to work
with a map of each object to the single target object that it orbits. We'll split each line by the `)` character that
separates the object from its orbiter, and then combine them all together into a map. Note that the `assoc` function
maps each orbiter to its source, which is the opposite order from how each object appears in the input.

```clojure
(defn parse-orbits [input]
  (reduce (fn [m line]
            (let [[source orbiter] (str/split line #"\)")]
              (assoc m orbiter source)))
          {}
          (str/split-lines input)))
```

Our goal is to count the number of direct and indirect orbits throughout the graph of objects orbiting the
center of mass (COM). The easiest way I thought to do this was to map each object to the path it has to take
to reach the COM, since the first such orbit would be the "direct" orbit and every additional one would be
"indirect." We'll make a function `path-to-com` that starts from an object and returns its sequence of orbits.

Note that we could benefit from caching values if we calculated all of the paths at the same time, but I
didn't. If A orbits B orbits C orbits COM, and we determine the path from B to COM, then we don't need to
recalculate most of the work when finding the path from A. I didn't bother with this since the calculation is
fast enough as it is for the input data.

```clojure
(defn path-to-com [orbits start]
  (->> start
       (iterate #(get orbits %))
       (take-while some?)
       rest))
```

Then for `part1`, we parse all the inputs, map each value to its path, count the path, and then add up the
path lengths. This time, as in day 1, I opted to use `transduce` on principle, and because even though it's still
new to me, it actually communicates the intention more cleanly than a pipeline. Here, we go through each orbit in the
map, run 3 transformations -- take the key, calculate the path, and count the length of the path -- and then combine the 
output values together using `+`.

```clojure
; Chosen solution using transduce.
(defn part1 [input]
  (let [orbits (parse-orbits input)]
    (transduce (map (comp count (partial path-to-com orbits) key))
               +
               orbits)))

; Equivalent solution 
(defn part1 [input]
  (let [orbits (parse-orbits input)]
    (->> orbits
         (map (comp count (partial path-to-com orbits) key))
         (reduce +))))
```

---

## Part 2

Now we need to calculate the path between the objects that two objects, `YOU` and `SAN`, are orbiting. The
approach is to find the first object that both `YOU` and `SAN` orbit in their path to the COM, and add those
distances together. It's actually a pretty straightforward once you draw it out.

To start, I'll make a utility function called `map-to-first-index`, which takes a collection and returns a map
of each value to its first index in the collection. While in this case each object should appear only once, this seems
like a generic function I might like to use again later.

```clojure
; advent-2019-clojure.utils namespace
(defn map-to-first-index [coll]
  (reduce (fn [m [idx v]] (if-not (m v) (assoc m v idx) m))
          {}
          (map-indexed vector coll)))
```
That's really all I needed before creating `part2`. In this case, we'll parse the orbits and take the path
from `SAN` to `COM`, converting it into its distance map using `map-to-first-index`. Then we take the path from
`YOU` and look for the first object that appears in the `from-san` map. Once we find it, we return the sum of
the two distances. I did make a solution with `reduce` and `reduced`, but it wasn't as clear; and the
`transduce` solution was just ridiculous. Sometimes, pipelines are just fine!

```clojure
(defn part2a [input]
  (let [orbits (parse-orbits input)
        from-san (utils/map-to-first-index (path-to-com orbits "SAN"))]
    (->> (path-to-com orbits "YOU")
         (keep-indexed (fn [idx v]
                         (when-let [d (from-san v)]
                           (+ d idx))))
         first)))
```