# Day 16: Flawed Frequency Transmission

* [Problem Statement](https://adventofcode.com/2019/day/16)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day16.clj)

---

## Part 1

Today we're processing signal transmissions, bit by bit.

To start off, we need to use a starting pattern and apply different values to it based on which index of a target
input we need to calculate. Namely, for position `n` in the input, we play each character of a base pattern `n` times,
and then drop the first value. For this, we'll use a combination of `cycle` to infinitely repeat the entire
`base-pattern`, `repeat` to make `n` copies of each character, and `mapcat` to flatmap all of the repeated characters
into a single sequence. 

```clojure
(def base-pattern [0 1 0 -1])

(defn pattern-at-index [index]
  (->> (cycle base-pattern)
       (mapcat #(repeat (inc index) %))
       rest))
```

Next we make two helper functions. The `last-digit` function returns the last character of a number, without its sign.
We have to use `rem` instead of `mod` here, because `(mod -1 10)` returns `9` instead of `-1` as we would want, and
of course `abs` makes the value positive. Second, to turn a numeric string into a sequence of single-digit numbers,
we leverage `Character/getNumericValue` on each character in the string `s`. 

```clojure
(defn last-digit [n]
  (abs (rem n 10)))

(defn numeric-array [s]
  (map #(Character/getNumericValue ^char %) s))
```

Now we can get to the primary function, `calc-signal`, which we will implement with two arities. The 1-argument version
maps each character in the sequence of digits to the 2-argument version, passing in the index in the second argument.
To calculate the new value of the digits in a particular index, we need to multiple each value in the digits by the
`pattern-at-index` at that position.  The 3-arity version of `map` can be used to multiple each digit against the
sequence of pattern digits, after which we sum up the products and convert down to the `last-digit`.

```clojure
(defn calc-signal
  ([digits]
   (map #(calc-signal digits %) (range (count digits))))

  ([digits index]
   (->> (pattern-at-index index)
        (map * digits)
        (apply +)
        last-digit)))
```

Finally, we can implement the `part1` function using the pieces already created. We'll parse the input using
`numeric-array`, create an infinite sequence of iterating the value of the 1-arity `calc-signal` function, grab the
100th iteration, and stringify the first 8 characters to get the answer.

```clojure
(defn part1 [input]
  (->> (numeric-array input)
       (iterate calc-signal)
       (drop 100)
       first
       (take 8)
       (apply str)))
```

On to part 2!

---

## Part 2

Part two starts off nicely, until it gets annoying.  To start, we need to calculate the message offset, by looking at
the numeric value of the first 7 digits of the input. Since that's a string, we use `subs` to get the substring, and
then parse it into a long value.

```clojure
(defn message-offset [digits]
  (->> (take 7 digits)
       (apply str)
       parse-long))
```

Now for the annoying part, which is another one of these puzzles where you need to know the trick to solve the problem.
You can read some other folks' write-ups about how this works, since I didn't figure it out myself, but here's the
way to solve this trick:
1. If the offset is value `n`, drop the first `n` digits of the expanded input, since those values will all converge
to be zeroes.
2. For each remaining digit at position `p`, the next value is `last-digit` calculation from summing all values in the
input vector from position `p` to the end. So the last digit repeats itself, the previous digit is its previous value
plus the last value, etc.

While I could have implemented this using the `map` function and a bunch of sub-vectors, it was more efficient to
`reduce` the values from the end of the vector, so we could accumulate the running sum. After going through all of the
digits, resulting in a list, we again convert it back into a vector.

```clojure
(defn calc-signal-extended [digits]
  (->> (reduce (fn [[acc sum] idx]
                 (let [sum' (last-digit (+ sum (digits idx)))]
                   [(conj acc sum') sum']))
               [() 0]
               (range (dec (count digits)) -1 -1))
       first
       vec))
```

And thus, the `part2` function is fairly simple. We'll quickly make a `repeat-signal` function that takes in the
`digits` vector, repeats it 10000 times into a single sequence, and then drops `message-offset` number of characters
before returning the result as a vector. So `part2` just parses the input, repeats it, iterates over
`calc-signal-extended` instead of `calc-signal`, and takes the first eight characters from the 100th iteration to get
to the solution.

```clojure
(defn repeat-signal [digits]
  (->> (repeat 10000 digits)
       (apply concat)
       (drop (message-offset digits))
       vec))

(defn part2 [input]
  (->> (numeric-array input)
       repeat-signal
       (iterate calc-signal-extended)
       (drop 100)
       first
       (take 8)
       (apply str)))
```

---

## Refactoring

Seen in this light, it shouldn't be too hard to figure out how to combine `part1` and `part2` into a unified solution.
It's easiest to look at the `solve` function first, which takes in two functions and the input. The first function is
a transformation of the input as a numeric array; part 1 doesn't need to modify the vector, but part 2 uses the
already-created `repeat-signal` transformation. The second function says which function to iterate over the converted
input, which is simply `calc-signal` or `calc-signal-extended`. Then the `solve` function stringifies the first 8
digits of the 100th iteration.

```clojure
(defn solve [input-fn iterate-fn input]
  (->> (iterate iterate-fn (input-fn (numeric-array input)))
       (drop 100)
       first
       (take 8)
       (apply str)))

(def part1 (partial solve identity calc-signal))
(def part2 (partial solve repeat-signal calc-signal-extended))
```