# Day 2: 1202 Program Alarm

* [Problem Statement](https://adventofcode.com/2019/day/2)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day02.clj)

---

## Part 1

For today's problem, we're building a computer call an Intcode, which takes in a list of integers and runs calculations
based on the data contained within it. For now, the program supports addition and subtraction, but a little
foreshadowing tells us we're going to add more features throughout the Advent. For the time being, I won't extract
anything into its own namespace quite yet, since I'd like to see another problem or two before we know what assumptions
are global vs specific to any one particular day.

To start out, the simplest way to represent the Intcode is with a map of `{:instruction-pointer n, :addresses []}`,
where the instruction pointer says the address of the next address to run, and the addresses are all of the memory
addresses. We'll start with parsing the input into this map, using `mapv` instead of `map` to ensure the addresses are
available for reading by offset.

```clojure
(defn parse-input [input] {:instruction-pointer 0
                           :addresses           (mapv parse-long (str/split input #","))})
```

Next we'll make a few convenience functions so the business logic doesn't have to deal directly with
the internals of an Intcode. There's nothing too fancy here -- `current-op` returns the next `opcode` to execute; 
`current-instruction` returns the next 4 values corresponding to the next instruction (the `opcode` plus its
parameters); `address-value` and `set-address-value` return and change the value at an address; and
`advance-instruction-pointer` moves the instruction pointer forward by 4, the length of a standard instruction.

```clojure
(def instruction-length 4)
(defn current-op [{:keys [instruction-pointer addresses]}]
  (get addresses instruction-pointer))
(defn current-instruction [{:keys [instruction-pointer addresses]}]
  (subvec addresses instruction-pointer (+ instruction-pointer instruction-length)))

(defn address-value [int-code address]
  (get-in int-code [:addresses address]))
(defn set-address-value [int-code address v]
  (assoc-in int-code [:addresses address] v))
(defn advance-instruction-pointer [int-code]
  (update int-code :instruction-pointer + instruction-length))
```

Next we'll move on to `run-instruction`, which we'll implement with multi-methods, since we expect to support
additional `opcodes` in future problems. The `run-instruction` multi-method dispatches to its individual implementations
based on the `current-op`, the function defined above. This means we need to implement `defmethod` implementations for
dispatch values of `1` (addition), `2` (multiplication), and `99` (termination). For addition and subtraction, we get
the values at the addresses specified in the two parameters after the `opcode`, and set them into the address in the
third parameter. Then we advance the instruction pointer to prepare for the next command. For the termination function,
I think that returning `nil` is good enough for now, since it shows that there is no next state. Maybe I'll regret this
in a future problem.

```clojure
(defmulti run-instruction current-op)
(defmethod run-instruction 1 [int-code]
  (let [[_ a b c] (current-instruction int-code)]
    (-> int-code
        (set-address-value c (+ (address-value int-code a)
                                (address-value int-code b)))
        (advance-instruction-pointer))))
(defmethod run-instruction 2 [int-code]
  (let [[_ a b c] (current-instruction int-code)]
    (-> int-code
        (set-address-value c (* (address-value int-code a)
                                (address-value int-code b)))
        (advance-instruction-pointer))))
(defmethod run-instruction 99 [_] nil)
```

Alright, there's some duplication there between addition and multiplication. Let's refactor their implementations into
a common `arithmatic-instruction` function that passes in either `+` or `*` to simplify the code.

```clojure
(defn- arithmatic-instruction [f int-code]
  (let [[_ a b c] (current-instruction int-code)]
    (-> int-code
        (set-address-value c (f (address-value int-code a)
                                (address-value int-code b)))
        (advance-instruction-pointer))))

(defmulti  run-instruction current-op)
(defmethod run-instruction 1 [int-code] (arithmatic-instruction + int-code))
(defmethod run-instruction 2 [int-code] (arithmatic-instruction * int-code))
(defmethod run-instruction 99 [_] nil)
```

Our goal is to find the value of the first address after the program fully runs, so let's create a `run-to-completion`
function to execute operations until the Intcode terminates.  For this we'll use `(iterate run-instruction int-code)`
to get a sequence of Intcode states, then call `(take-while some?)` to only use the non-nil values, and then `last` to
take the final state before the Intcode stopped. This should return an Intcode state.

```clojure
(defn run-to-completion [int-code]
  (->> (iterate run-instruction int-code)
       (take-while some?)
       last))
```

To solve the problem, we'll parse the input, set the two addresses to specific values per the instructions, run the
Intcode to completion, and then return the address value at position 0.  Easy enough.

```clojure
(defn part1 [input]
    (-> (parse-input input)
        (set-address-value 1 12)
        (set-address-value 2 2)
        (run-to-completion)
        (address-value 0)))
```

---

## Part 2

For this part, we need to determine which initial values for addresses 1 and 2, named `noun` and `verb`, we need to set
to obtain a target output of `19690720`. We have most of the pieces already in place, but first let's do a little
refactoring. We're going to create a function `output` which takes in an Intcode and its noun and verb, sets the two
addresses, runs the program, and returns the first address value.  With this in place, it's easy to refactor the
`part1` function to leverage it.

```clojure
(defn output [int-code noun verb]
  (-> int-code
      (set-address-value 1 noun)
      (set-address-value 2 verb)
      (run-to-completion)
      (address-value 0)))

(defn part1 [input] (-> input parse-input (output 12 2)))
```

Now for part 2, we know that the noun and verb are both values between 1 and 99, inclusively. The `for` list 
comprehension should help us go through all of the possible values. We'll use the `:when` tag to find the noun/verb
combinations that outputs the correct value; hopefully there's only one such combination. Once we find it, we'll run
the calculation of `100 * noun + verb` and return the first value.

```clojure
(defn part2 [input]
  (let [int-code (parse-input input)]
    (first (for [noun (range 1 100)
                 verb (range 1 100)
                 :when (= (output int-code noun verb) 19690720)]
             (-> 100 (* noun) (+ verb))))))
```