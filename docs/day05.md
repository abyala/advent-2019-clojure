# Day 5: Sunny with a Chance of Asteroids

* [Problem Statement](https://adventofcode.com/2019/day/5)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day05.clj)
* [Intcode code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/intcode.clj)

---

## Part 0

Ooh, lordy, we had a lot of code to write for this one!  We're building on the IntCode source from Day 2, and then we
made a bunch of extensions and refactorings. So before we do anything with the real code, we're going to move most of
the Day 2 code into the `intcode` namespace. This includes all of the logic for parsing, `current-op`, 
`current-instruction`, everything having to do with addresses and the insturuction pointer, the three supported
instructions (1, 2, and 99), and `run-to-completion`.  When all is said and done, the `day02` namespace only contains
the problem-specific `output` function, along with `part1` and `part2`.

Note that since we're building up the IntCode namespace, the bulk of the test cases go into `intcode-test` now,
and we'll be building up a sizable number of tests with today's problem!

---

## Part 1

The Day 5 problem statement introduces a bunch of concepts -- two new instructions, the concept of input and output
data, and parameter modes. Let's take these one at a time.

First, let's think about inputs and outputs. We're told that there's only a single input value, here the value `1`, but
we're going to generate several output values that correspond to the diagnostic tests, followed by the diagnostic code.
Let's assume that we'll eventually have to handle multiple inputs, and thus let `intcode` handle multiple values of
both. So to start, we'll expand the `parse-input` function to take in an optional vector of input values; we'll keep
the existing 1-arity function so that the Day 2 code doesn't change.

While we're at it, we can see that the load and store commands coming up need to be able to write to arbitrary memory
addresses, since `"3,50"` is an acceptable program. So we'll refactor the `addresses` in the Intcode to be a map of
`{address value}`. Here's something neat - `str/split` evaluates the string eagerly, returning a vector, and
`reduce-kv` can read a vector by using the vector's index and value be the reduction function's paramters for the key
and value, respectively.

```clojure
(defn parse-input
  ([s] (parse-input s []))
  ([s inputs] {:instruction-pointer 0
               :addresses           (reduce-kv (fn [m k v] (assoc m k (parse-long v))) {} (str/split s #","))
               :inputs              inputs
               :outputs             []}))
```

Since inputs are provided up-front and are used only once, it looks like we only need three helper functions.
`add-output` appends a value to the end of the output vector. `inputs` just returns all of the (yet-unread) input
values, and `drop-inputs` removes one or more inputs as we consume them in upcoming functions.

```clojure
(defn add-output [int-code v]
  (update int-code :outputs conj v))
(defn inputs [int-code]
  (:inputs int-code))
(defn drop-inputs
  ([int-code] (drop-inputs int-code 1))
  ([int-code n] (update int-code :inputs (partial drop n))))
```

Before we get to the new instructions for reading input and writing output, we need to first handle parameter modes,
which are read from the opcode value but affect the parameters for each request. Because each command uses the
parameters a little differently, let's implement `current-params` to provide metadata about each parameter, namely the
vector `[{:mode [:position|:immediate], :value n}]`. We'll first grab all the parameters needed by the instruction
by using `select-keys` to find those values by name; remember that since `:addresses` is no longer a vector, `subvec`
will no longer work. Once we have that new map of desired fields, we sort them by their index values (`first`), and
pull out the vector of their actual values `(mapv second)`. Next, we look to the instruction argument to find all the
parameter modes, by converting the value back into a string, reversing it, and dropping the first two values which
correspond to the opcode. Finally, we return a new vector by combining the mode types with the parameter values,
converting the `0` or `1` into `:position` or `:immediate` for clarity.

Fun fact - for the parameters, we could do `(vec (vals (select keys...)))` because when `select-keys` pulls out fewer
than 8 parameters, it returned an ArrayMap instead of a HashMap, which means the parameters will be in order. It works,
but I sure wouldn't want to depend on that.

```clojure
(def ^:private mode-types {\0 :position, \1 :immediate})
(defn current-params [{:keys [instruction-pointer addresses]} num-params]
  (let [params (->> (select-keys addresses (range (inc instruction-pointer) (+ instruction-pointer num-params 1)))
                    (sort-by first)
                    (mapv second))
        modes (->> (get addresses instruction-pointer) str reverse (drop 2) vec)]
    (mapv (fn [n] {:mode  (mode-types (get modes n \0))
                   :value (params n)})
          (range num-params))))
```

While we're here, let's add two convenience functions, `param-value` and `param-immediate-value` to calculate these
parameters when they're needed. `param-value` returns either the positional or immediate value of the parameter, while
`param-immediate-value` just grabs the immediate value by assuming that's what's in the parameter.

Note: the instructions explicitly state "Parameters that an instruction writes to will never be in immediate mode." I
find this to be a bizarre way of thinking of the problem. It's true that when you write data into an address, it goes
to an address and not a scalar value. However, when I read the code, positional mode means "look at what's at this
address and work with the value there," while immediate mode means "just use this value." If I have an instruction
`"1,2,3,4"`, then I'm going to add the values from addresses 2 and 3 and put them into address 4, not the address
that's currently stored in address 4. It may just be me, but we'll see later that my write operations use
`param-immediate-value` all the time.

```clojure
(defn param-value [int-code {:keys [mode value]}]
  (case mode
    :position (address-value int-code value)
    :immediate value))
(def param-immediate-value :value)
```

Ok, _now_ we can fix the `arithmetic-instruction`, including fixing the typo I had the first time. Now, we'll use
`current-params` to pull out the 3 parameters, apply the addition or multiplication to the `param-value` calculations
for the first two paramters, and then store the result into the `param-immediate-value` of the third parameter.
Finally, since we know that not all instructions will have 3 arguments going forward, the call to
`advance-instruction-pointer` now takes in an argument of how many positions to move.

```clojure
(defn advance-instruction-pointer [int-code distance]
  (update int-code :instruction-pointer + distance))

(defn- arithmetic-instruction [f int-code]
  (let [[a b c] (current-params int-code 3)]
    (-> int-code
        (set-address-value (param-immediate-value c) (f (param-value int-code a)
                                                        (param-value int-code b)))
        (advance-instruction-pointer 4))))
```

Whew!  We can finally implement the `input-instruction` and `output-instruction` using the tools we have already seen.
To read an input, we'll take the first value from `(inputs)`, set it into the immediate value of the first parameter,
drop that input value, and move the instruction pointer twice. Similarly, for outputs we'll read the value from the
first parameter, output it, and again move the instruction pointer twice. And of course we need to wire these up to
opcodes `3` and `4` by extending the `run-instruction` multimethod.

```clojure
(defn- input-instruction [int-code]
  (let [[input] (inputs int-code)
        address (-> int-code (current-params 1) first param-immediate-value)]
    (-> int-code
        (set-address-value address input)
        (drop-inputs)
        (advance-instruction-pointer 2))))
(defn- output-instruction [int-code]
  (let [output (param-value int-code (-> int-code (current-params 1) first))]
    (-> int-code
        (add-output output)
        (advance-instruction-pointer 2))))

(defmethod run-instruction 3 [int-code] (input-instruction int-code))
(defmethod run-instruction 4 [int-code] (output-instruction int-code))
```

Finally, we can implement the `part1` function back in the `day05` namespace. We'll create an `intcode` with the
provided instructions and the single input value `1`, run the program and get the outputs. If the diagnostic values
(all outputs besides the last one) are zeros, we return the final value. Otherwise, there's an error and the function
returns a `:diagnostic-error` to investigate.

```clojure
(defn part1 [input]
  (let [intcode (ic/parse-input input [1])
        output (-> intcode ic/run-to-completion :outputs)]
    (if (every? zero? (butlast output))
      (last output)
      {:diagnostic-error (butlast output)})))
```

---

## Part 2

The hard part is out of the way, so now we're just adding more instructions. Per the instructions, we now need to
support two jump instructions and two comparison instructions.  Easy.

For the jump instructions, `jump-if-true` and `jump-if-false`, we need to run a comparison function on the first
parameter. If the function passes, we jump to the instruction defined in the second parameter. Otherwise, we proceed
to the next instruction 3 addresses a way. `jump-if-false` passes in the predicate function `zero?`, and for
`jump-if-true` we can use a simple `(complement zero?)` instead of the more complex `(partial not= 0)` definition.
Note that we also need the function `set-instruction-pointer` to complement the existing `advance-instruction-pointer`.

```clojure
(defn set-instruction-pointer [int-code value]
  (assoc int-code :instruction-pointer value))

(defn- jump-if-instruction [pred int-code]
  (let [[a b] (current-params int-code 2)]
    (if (pred (param-value int-code a))
      (set-instruction-pointer int-code (param-value int-code b))
      (advance-instruction-pointer int-code 3))))

(defmethod run-instruction 5 [int-code] (jump-if-instruction (complement zero?) int-code))
(defmethod run-instruction 6 [int-code] (jump-if-instruction zero? int-code))
```

For the comparison instructions, we similarly need to compare the values defined in the first two parameters against
one of two comparison functions. If the predicate passes, we set a 1 in the immediate address value of the third
argument, else we store a 0. Then we always move forward 4 addresses for the opcode and the 3 parameters. 
Unsurprisingly, the comparison function for `less than` is `<` and for `equals` is `=`.

```clojure
(defn- compare-instruction [pred int-code]
  (let [[a b c] (current-params int-code 3)]
    (-> int-code
        (set-address-value (param-immediate-value c) (if (pred (param-value int-code a)
                                                               (param-value int-code b))
                                                       1 0))
        (advance-instruction-pointer 4))))

(defmethod run-instruction 7 [int-code] (compare-instruction < int-code))
(defmethod run-instruction 8 [int-code] (compare-instruction = int-code))
```

Finally, we can implement `part2` by using the input value of `5` instead of `1`, and returning the single output 
value. I made a helper function `program-outputs` that takes in the initial addresses and the (single) input value,
and returns all of the output values.  With this, we've finished our large amounts of refactorings, and have a shiny
new Intcode system!

```clojure
(defn program-outputs [program single-input]
  (-> (ic/parse-input program [single-input])
      (ic/run-to-completion)
      :outputs))

(defn part1 [input]
  (let [output (program-outputs input 1)]
    (if (every? zero? (butlast output))
      (last output)
      {:diagnostic-error (butlast output)})))

(defn part2 [input]
  (first (program-outputs input 5)))
```