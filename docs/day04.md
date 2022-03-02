# Day 4: Secure Container

* [Problem Statement](https://adventofcode.com/2019/day/4)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day04.clj)

---

## Part 1

Today's problem could be renamed to "fun with predicates." We're given a numeric range, and need to count the number of
values within that range which all pass some validation rules, password rules in this case. Even though the passwords
are all numbers, we'll define the predicates assuming that they are numeric strings instead.

The `six-digit?` predicate just checks if the length of t he input value is 6. Technically speaking, this is
unnecessary since the min and max values in the input are both 6-digit numbers, but I suppose they might not be for
some inputs. So this function is trivial.

The `any-two-adjacent?` predicate looks at each pair of characters within the string, using the `(partition 2 1 s)`
syntax, where the `2` is the size of each partitioned list, and the `1` is the step size, allowing us to look at every
overlapping pair. To check if any pair has identical values, we'll use `(some (partial apply =) coll)`, where `some`
is the oddly named Clojure function that returns a truthy value if the predicate holds for any value in the collection.

Finally, `never-decreases?`, which I know I named as a negative predicate (sue me), uses a similar strategy to see if
the characters only increase in value. We'll need to map each character in the string to an integer, and in this case
I use the normal `int` function to convert to the ASCII code instead of the digit value, since both approaches
increment correctly from 0 to 9. Then this time, instead of using `some`, we'll use `not-any?` to filter out any
characters that decrease in value.

```clojure
(defn six-digit? [s]
  (= 6 (count s)))
(defn any-two-adjacent? [s]
  (some (partial apply =) (partition 2 1 s)))
(defn never-decreases? [s]
  (->> (map int s)
       (partition 2 1)
       (not-any? (partial apply >))))
```

To put it all together, the `part1` function looks at the sequence of values from low to high (inclusive), maps each
number into a string, and then filters those strings to the ones that match all of the predicates defined above. The
`every-pred` function does this cleanly, as it takes in one or more predicates and returns a new predicate that returns
true if all of its predicates pass. Finally, we count the number of strings that make it through the predicate, to get
to our answer.

```clojure
(defn part1 [low high]
  (->> (range low (inc high))
       (map str)
       (filter (every-pred six-digit? any-two-adjacent? never-decreases?))
       count))
```

---

## Part 2

Well this is nice - for part 2, we just need a different predicate to replace `any-two-adjacent?`, such that we only
allow strings with at least one adjacent pair (not triple) of identical characters. This does also include the first
or last two characters, so it's a little trickier, but not much.

The `exactly-two-adjacent?` function places a non-numeric character at the front and the back of the string, to allow
the function to use a partition window of `4` instead of  `2`. Then its predicate checks that the middle two characters
are equal, but they don't match the first or last character. Any matching triple would have either the first or last
character match one of the middle values, so this gets the filter done.

```clojure
(defn exactly-two-adjacent? [s]
  (->> (str "~" s "~")
       (partition 4 1)
       (some (fn [[a b c d]] (and (not= a b) (= b c) (not= c d))))))
```

Then to implement `part2`, we just change the predicate list in the `every-pred` call.

```clojure
(defn part2 [low high]
  (->> (range low (inc high))
       (map str)
       (filter (every-pred six-digit? exactly-two-adjacent? never-decreases?))
       count))
```

---

### Refactoring 1

Well I suppose we _should_ define a common function that takes in _all_ of the predicates we need, but I'll opt for
concision this time. The `solve` function takes in the adjacency predicate we want to apply, plus the low and high
values, and then it does all the work. `adjacency-fn` fits in nicely within the `every-pred` call, and then `part1`
and `part2` become partial functions that pass in their adjacency functions of `any-two-adjacent?` and
`exactly-two-adjacent?`, respectively.

```clojure
(defn solve [adjacency-fn low high]
  (->> (range low (inc high))
       (map str)
       (filter (every-pred six-digit? adjacency-fn never-decreases?))
       count))

(def part1 (partial solve any-two-adjacent?))
(def part2 (partial solve exactly-two-adjacent?))
```

---

### Refactoring 2

Reading [Todd Ginsberg's Kotlin solution](https://todd.ginsberg.com/post/advent-of-code/2019/day4/) to this problem,
I like another improvement he made to the adjacency function. Instead of creating 2- or 4-element vectors, he used
the Kotlin function `groupBy`, which in Clojure is `partition-by`, to tease apart all identical values into 
sub-sequences. Then `any-two-adjacent?` and `exactly-two-adjacent?' amount to how the code evaluates these sequences.

The new function `any-adjacent?` takes in the comparison function and the string, and returns
`(some f (partition-by identity s))`. And the `solve` function now takes in the comparison function and passes it in to
`any-adjacent?`. This makes the final code actually quite small and reusable for future wacky password rules!

```clojure
(defn six-digit? [s]       (= 6 (count s)))
(defn any-adjacent? [f s]  (some f (partition-by identity s)))
(defn never-decreases? [s] (->> (map int s)
                                (partition 2 1)
                                (not-any? (partial apply >))))

(defn solve [adjacency-fn low high]
  (->> (range low (inc high))
       (map str)
       (filter (every-pred six-digit? (partial any-adjacent? adjacency-fn) never-decreases?))
       count))

(def part1 (partial solve #(> (count %) 1)))
(def part2 (partial solve #(= (count %) 2)))
```

Heck at this point, we're not reusing the predicates, so we can collapse them into a simple `and` function. This gives
us a single `solve` function with its comparison function passed in by `part1` and `part2`.

```clojure
(defn solve [adjacency-fn low high]
  (->> (range low (inc high))
       (map str)
       (filter (fn [s] (and (= 6 (count s))
                            (some adjacency-fn (partition-by identity s))
                            (->> (map int s)
                                 (partition 2 1)
                                 (not-any? (partial apply >))))))
       count))

(def part1 (partial solve #(> (count %) 1)))
(def part2 (partial solve #(= (count %) 2)))
```
