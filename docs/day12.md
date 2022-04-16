# Day 12: The N-Body Problem

* [Problem Statement](https://adventofcode.com/2019/day/12)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day12.clj)

---

## Preface

For this problem, after solving Part 1, I decided to flip the data structures around pretty substantially. Rather than
pretend I had the design right the whole time, I'm going to show my original solution, then refactor it, and then do
part 2.  Feel free to skip this original section if the initial code isn't interesting, since it won't appear in the
final Git submission.

---

## Part 1, First Solution

In this problem, we're going to look at multiple moons that apply gravitational pulls on each other, and measure where
they end up after a unit of time.

Given a list of initial triordinates and initial velocities of all zero, the data structure I thought most appropriate
for each moon was `{:x [pos velo], :y [pos velo], :z [pos velo]}`, thus capturing each dimensions data all together.
Theoretically, I could have been more verbose with `{:x {:position p, :velocity v}, ...}` but that seemed unnecessary.
So we'll start by parsing each line using the `parse-moon` function, leveraging `zipmap` to name each dimension `:x`,
`:y`, and `:z`.

```clojure
(def all-dimensions [:x :y :z])

(defn parse-moon [line]
  (->> line
       (re-seq #"-?\d+")
       (mapv (comp #(vector % 0) parse-long))
       (zipmap all-dimensions)))

(defn parse-input [input]
  (->> input str/split-lines (map parse-moon)))
```

Next, we'll make two separate functions, called `apply-gravity` and `apply-velocity`, and their collection functions
`apply-gravities` and `apply-velocities`.

For `apply-gravity`, I decided to give this function two arities. The 2-arity version takes two moons, applying the
gravity of the second onto the first; and the 3-arity version takes two moons and the dimension to apply in the
comparison. The `apply-gravities` functions maps each moon to the reduction of calling the 2-arity version of
`apply-gravity` with each other moon. The 2-arity version reduces the 3-arity version between two moons for each
dimension. And the 3-arity version grabs the first vector element for the given dimension (the first element being the
position and the second the velocity), calls `compare` to get back a -1, 0, or 1, and then adds that value to the 
second element of the moon's dimension, thus changing its velocity. Note that we are completely safe to compare a moon
to itself, since the comparison will just return 0 anyway.

```clojure
(defn apply-gravity
  ([moon1 moon2] (reduce (fn [acc dim] (apply-gravity acc moon2 dim))
                         moon1
                         all-dimensions))
  ([moon1 moon2 dim] (->> (map (comp first dim) [moon2 moon1])
                          (apply compare)
                          (update-in moon1 [dim 1] +))))

(defn apply-gravities [moons]
  (map #(reduce apply-gravity % moons) moons))
```

The `apply-velocity` function is much simpler. For each dimension, we simply add the velocity of the dimension to its
position. Then `apply-velocities` calls `apply-velocity` for each moon.

```clojure
(defn apply-velocity [moon]
  (reduce (fn [acc dim] (update-in acc [dim 0] + (get-in acc [dim 1])))
          moon
          all-dimensions))
(defn apply-velocities [moons]
  (map apply-velocity moons))
```

Thus a single step involves simply applying gravities and then velocities to all of the moons.

```clojure
(defn take-step [moons]
  (-> moons apply-gravities apply-velocities))
```

Calculating the total energy of a moon involves multiplying the potential and kinetic energies. Since we don't care
about those energies, I made a single `energy` function that can apply to the `first` value (potential) or `second`
value (kinetic) of a moon.  Then we call both energy values on that moon, and multiply the results.

```clojure
(defn total-energy [moon]
  (letfn [(energy [f] (reduce + (map (comp abs f) (vals moon))))]
    (* (energy first) (energy second))))
```

Finally, we can implement the `part1` function. I _really_ wish that `nth` put the step number before the collection,
like `take` and `drop` do, but this is beyond my control, so we don't make a very simple pipeline. But we'll parse
the moons, send them into an `iterate` function, grab the `nth` value out of it (1000 for the real puzzle), calculate
the total energy, and add the values up for all of the moons.

```clojure
(defn part1 [input num-steps]
  (let [moons (parse-input input)]
    (->> (nth (iterate take-step moons) num-steps)
         (map total-energy)
         (reduce +))))
```

---

## Part 2, Not Really

Part 2 _should_ be very simple, but it falls apart given long-running inputs. The naive solution is this simple
function.

```clojure
(defn part2-will-not-complete [input]
  (reduce (fn [seen state] (if (seen state)
                             (reduced (count seen))
                             (conj seen state)))
          #{}
          (iterate take-step (parse-input input))))
```

The trick to this problem is to recognize that the `x`, `y`, and `z` dimensions move independently, and so we don't
actually need to find the first state where we've seen all three positions and velocities together. Instead, we find
out how long it takes for each of them to loop, and then use the Least Common Multiple (LCM) value to calculate where
all three of them loop. I say this as though I figured it out myself; I understand the rationale, but still, thank you
to folks who posted their solution, since I don't like the mathy parts of AoC!

---

## Part 1, Refactored

To prepare for Part 2, we'll refactor pretty much all of Part 1 into a less intuitive data structure. This time,
since we don't need to look at an entire moon all together until the very end, we'll represent all of the moons as
`([[x0 x1 x2 x3] [dx0 dx1 dx2 dx3]], [[y0 y1 y2 y3] [dy0 dy1 dy2 dy3]], [[z0 z1 z2 z3] [dz0 dz1 dz2 dz3]])`. This will
be a three-element list of `x`, `y`, and `z`, where each dimension is a two-element vector of all positions and all
velocities.

The `parse-input` function is a bit more complex now, since we need to read each moon and split apart its dimensions.
For this, we'll start with the same `(re-seq #"-?\d+" line)` parsing function as before for each line, resulting in
a sequence of three-element longs. Then we `interleave` those lines, sticking all of the `x` values next to each other,
then the `y` values, and then the `z` values. We call `(partition num-lines)` to assemble the three dimensions, and
then map each dimension list to the vector of its positions and a vector of zeros of the correct length.

```clojure
(defn parse-input [input]
  (let [lines (str/split-lines input)
        num-lines (count lines)]
    (->> lines
         (map (fn [line] (->> line
                               (re-seq #"-?\d+")
                               (mapv parse-long))))
         (apply interleave)
         (partition num-lines)
         (map #(vector (vec %) (vec (repeat num-lines 0)))))))
```

The `move-moons` function, in contrast, is much simpler than the `apply-gravity` and `apply-velocity` functions, since
we look at each dimension in isolation. The function will take in a dimension, expressed as a vector of positions and
velocities. First, it identifies how each velocity will change similar to how we did it the first time - for each moon,
call `compare` between its position and all other moons, and reduce each comparison by `+` to determine the total
change in velocity. Then, update all of the velocities by adding their velocity changes, and update all of the positions
by adding their new velocities.

```clojure
(defn move-moons [[positions velocities]]
  (let [velocity-changes (map (comp (partial reduce +)
                                    (fn [pos] (map #(compare % pos) positions)))
                              positions)
        new-velocities (mapv + velocities velocity-changes)
        new-positions (mapv + positions new-velocities)]
    [new-positions new-velocities]))
```

Calculating the total energy for part 1 becomes tricky again, since we need to untangle the moons out of their
dimension lists, but it's not too terrible. The `energy-calc` function is a helper function that both `potential-energy`
and `kinetic-energy` will use, where the former uses `first` to get the positions and the latter uses `second` to get
the velocities. The `energy-calc` function maps each dimension to `first` or `second`, leaving three vectors of either 
positions or velocities, for each dimension. `interleave` will group the dimensions back into each moon, and `abs`
handles all negative numbers at once. Then we partition the values back up into each moon, and add the values up to get
the energy for all moons as a new collection.

Finally, for the `total-energy` function, we calculate the potential and kinetic energy for each moon, multiply those
values together, and then add together the total energy for each moon.

```clojure
(defn energy-calc [f moons]
  (->> moons
       (map f)
       (apply interleave)
       (map abs)
       (partition (count moons))
       (map (partial apply +))))
(defn potential-energy [moons] (energy-calc first moons))
(defn kinetic-energy   [moons] (energy-calc second moons))

(defn total-energy [moons]
  (->> moons
       ((juxt potential-energy kinetic-energy))
       (apply (partial map *))
       (reduce +)))
```

Now to get to the new and improved `part1` function, I'll create a `moon-movement-seq` function, which returns a lazy
sequence of each progression of the moons.  All we have to do is call `move-moons` on each iteration of the moons.

```clojure
(defn moon-movement-seq [moons]
  (iterate #(map move-moons %) moons))
```

Ok, let's rewrite `part1`.  We simply parse the input, create the sequence of moons, grab the `nt`h value, can return
the total energy.

```clojure
(defn part1 [input num-steps]
  (-> input parse-input moon-movement-seq (nth num-steps) total-energy))
```

---

## Part 2, For Real

Well now there isn't much to do. First off, we're going to assume that from the `moon-movement-seq` created in part 1,
we'll be able to make a function `first-cycle` that takes in a sequence of a _single dimension_ from that sequence,
and return how many steps it takes to get to the first cycle. This is a simple `reduce` call, taking in that infinite
sequence, reducing over the set of states already seen, and returning the number of states discovered as soon as the
sequence returns a known state. Recall that each state will be a vector of both positions and velocities.

```clojure
(defn first-cycle [dim-seq]
  (reduce (fn [seen state] (if (seen state)
                             (reduced (count seen))
                             (conj seen state)))
          #{}
          dim-seq))
```

We're also going to need to use the Least Common Multiple function, and rather than rewrite it again for AoC, I just
leverage the `org.clojure/math.numeric-tower` library, which conveniently implements `lcm` for us. However, because
I'm a dork and I want to use the `transduce` function, I'll have to make a reducing function wrapper around the `lcm`
function. It's really not bad at all. The 0-arity version (state initializer) returns a `1` as the identity value for
`lcm`; the `1-arity` version (completer) just returns the accumulated value; the `2-arity` version (reducer) calls the
actual `math/lcm` function.

```clojure
; advent-2019-clojure.utils namespace
(defn lcm-rf
  ([] 1)
  ([result] result)
  ([result input] (math/lcm result input)))
```

This allows for a very simple `part2` function, which sets up the `moon-seq` and calls a single `transduce` call for
each of the three dimensions that show up in each moon state.

```clojure
(defn part2 [input]
  (let [moon-seq (-> input parse-input moon-movement-seq)]
    (transduce (map (fn [dim] (->> moon-seq
                                   (map #(nth % dim))
                                   first-cycle)))
               lcm-rf
               (range 3))))

; If we hadn't made the reducing function and used reduce instead of transduce, the function would have been uglier.
; I think the solution above is cleaner, since LCM should be reducible outside of the context of this problem.
(defn part2 [input]
  (let [moon-seq (-> input parse-input moon-movement-seq)]
    (reduce (fn [acc dim] (math/lcm acc (->> moon-seq
                                             (map #(nth % dim))
                                             first-cycle)))
            1
            (range 3))))
```
