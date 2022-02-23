# Day 1: The Tyranny of the Rocket Equation

* [Problem Statement](https://adventofcode.com/2019/day/1)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day01.clj)

---

## Part 1

This puzzle was just focused on some simple math as we spin up the season of Advent. Given a list of modules for a
spaceship, calculate the total amount of fuel needed. To do this we sum together a fuel calculation for each module
in the input file.

There's nothing too complex here to get started. We'll make `fuel-required` to calculate the fuel needed for an amount
of mass. Then we take the input file, split it into lines of text, convert each line to a long value and run the
`fuel-required` function, and then add it all back together again with `reduce`. Note the use of the `comp` function
to combine the two mappings -- `parse-long` and `fuel-required` -- into a single, aggregate composed mapping function.

```clojure
(defn fuel-required [mass]
  (-> mass (quot 3) (- 2)))

(defn part1 [data]
  (->> (str/split-lines data)
       (map (comp fuel-required parse-long))
       (reduce +)))
```

---

## Part 2

For this part, we now need to include the fuel required to move the fuel that's needed to transport each module, and to
continue that calculation recursively. We could do an actual tail recursive function like this if we wanted:

```clojure
(defn recursive-fuel-required [mass]
  (loop [m mass, total-fuel 0]
    (let [fuel (fuel-required m)]
      (if (pos-int? fuel)
        (recur fuel (+ total-fuel fuel))
        total-fuel))))
```

Contextually, it should seem obvious that that's not my plan. Instead of using `loop-recur`, we can use `iterate` to
feed each fuel calculation back into the `fuel-required` function, until we no longer need more fuel. By getting a 
sequence of fuel amounts, we skip the initial value using `rest` (since it represents the initial mass, not the initial 
fuel), use `(take-while pos-int?)` to keep including positive fuel values, and then `(reduce +)` at the end to sum it 
all up. This gives us a nice, simple function that doesn't use recursion despite its name!

```clojure
(defn recursive-fuel-required [mass]
  (->> (iterate fuel-required mass)
       rest
       (take-while pos-int?)
       (reduce +)))
```

To write out `part2`, we'll make a common `solve` function that's usable by both parts 1 and 2, as that's a common
theme in my Advent solutions. For both parts, we take the input, split it into lines, parse each line into a long value,
then run one of the two fuel calculations before adding together all of the fuel. Part 1 uses `fuel-required` and Part
2 uses `recursive-fuel-required`.  This leaves us with a nice final state of the problem.

```clojure
(defn solve [calc input]
  (->> (str/split-lines input)
       (map (comp calc parse-long))
       (reduce +)))

(def part1 (partial solve fuel-required))
(def part2 (partial solve recursive-fuel-required))
```

---

## Refactoring

I realized after completing this project that the `solve` function is an excellent use case for a transducer. I can't
find a description of a transducer that normal humans can understand, so here's my explanation -- given a sequence of
values, transduction isolates the concepts of "what should I do to each value as I read it" from "how do I combine
the results of the first step into a final result." In this case, the first step for both `part1` and `part2` is to
parse each line and run a calculation, and the second step is to add the values together.

The `transduce` function takes in 3 or 4 arguments, and I use 3 in this scenario. The first argument is the function
to apply to each value as it is processed, so in this case that is `(map (comp calc parse-long))` as we saw in the
original solution. The second argument is the reducing function to apply, so that's just `+`. I'm omitting the
initial value argument, since the `+` function will correctly use a default value of `0` automatically. Finally, the
last argument is the input sequence, which is `(str/split-lines input)`. Thus, we have a very simple one-liner for the
`solve` function which, under the hood, might even be more efficient than the original pipeline.

```clojure
(defn solve [calc input]
  (transduce (map (comp calc parse-long)) + (str/split-lines input)))
```