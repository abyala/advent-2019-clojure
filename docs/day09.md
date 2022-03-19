# Day 9: Sensor Boost

* [Problem Statement](https://adventofcode.com/2019/day/9)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day09.clj)
* [Intcode code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/intcode.clj)

---

## Part 1

More updates to our Intcode program!  Today we're adding relative mode to go along with position and immediate modes.
This just means our Intcode system has to keep track of its relative base value, and using it for address-base
operations when needed.

To start off, we'll add a `:relative-base` property to our Intcode data structure, with an internal function called
`relative-base` to access its value.

```clojure
(defn parse-input [s]
  (let [out-chan (chan)
        out-mult (mult out-chan)
        out-copy-chan (chan 1000)]
    (tap out-mult out-copy-chan)
    {:instruction-pointer 0
     :relative-base       0                     ; This is the only changed line
     :addresses           (reduce-kv (fn [m k v] (assoc m k (parse-long v))) {} (str/split s #","))
     :input               (chan 1000)
     :output              {:channel   out-chan
                           :mult      out-mult
                           :copy-chan out-copy-chan}}))

(defn relative-base [int-code]
  (:relative-base int-code))
```

Then we need to know how to apply this to our operations. We'll map the `mode-type` value of `2` to the keyword
`:relative`, for use in the `current-params` function. Then we expand the `param-value` function to support that
keyword, such that it returns the address value at the sum of the parameter's value and the relative base; the code
should look very similar to the `:position` code.

Finally, remember that I find the interpretation of how to write values to be confusing, where the instructions used to
treat them all as though they were `position` values even though I thought that `immediate` made more sense. Well now
we need to support `relative` values here too. So we're going to replace the `param-immediate-value` function with
`param-address-value`, which returns either the value of the parameter or its value added to the relative base, with
the understanding that that value will be used to reference an address later for writing.

```clojure
(def ^:private mode-types {\0 :position, \1 :immediate, \2 :relative})

(defn param-value [int-code {:keys [mode value]}]
  (case mode
    :position (address-value int-code value)
    :immediate value
    :relative (address-value int-code (+ value (relative-base int-code)))))
(defn param-address-value [int-code {:keys [mode value]}]
  (if (= :relative mode)
    (+ (relative-base int-code) value)
    value))
```

That code will be used to refactor `arithmetic-instruction`, `input-instruction`, and `compare-instruction`. I won't 
put all three implementations here, but here is `arithmetic-instruction` as an example:

```clojure
(defn- arithmetic-instruction [f int-code]
  (let [[a b c] (current-params int-code 3)
        target-address (param-address-value int-code c)]             ; This used to be parameter-immediate-value
    (-> int-code
        (set-address-value target-address (f (param-value int-code a)
                                             (param-value int-code b)))
        (advance-instruction-pointer 4))))
```

Now we can add the new function `adjust-relative-base` for opcode `9`. All we need to do is read the single parameter
value, and increment the relative base by that amount. Simple enough.

```clojure
(defn- adjust-relative-base [int-code]
  (let [adjustment (param-value int-code (-> int-code (current-params 1) first))]
    (-> int-code
        (update :relative-base + adjustment)
        (advance-instruction-pointer 2))))

(defmethod run-instruction 9 [int-code] (adjust-relative-base int-code))
```

The only other fairly minor change is to enhance `address-value` to support a default value of `0` instead of `nil`,
since the problem says we need to support not only arbitrary memory values, but to treat unset values as zero.

```clojure
(defn address-value [int-code address]
  (get-in int-code [:addresses address] 0))
```

Alright - we can write the `part1` function now! A successful implementation should return a single output value
after we inject an input value of `1`; anything else means we have failing opcodes.  For the record, serious kudos to
the creator of Advent Of Code, Eric Wastl, for the incredible amount of work it must have taken for him to generate
such an excellent problem definition. As I was debugging, these failure codes told me precisely which operations were
wrong, such that I could expand my test suite accordingly.

```clojure
(defn part1 [input]
  (let [outputs (-> input ic/parse-input (ic/add-input 1) ic/run-to-completion ic/outputs)]
    (if (= 1 (count outputs))
      (first outputs)
      {:failing-opcodes outputs})))
```

---

## Part 2

Part 2 only differs from part 1 by the input value.  I'm not sure why the instructions suggest that the program would
run slowly if there were a bad algorithm, but my solution blazed along. So I'll just create a `solve` function to
unify the logic between the two parts, and then call it a day!

```clojure
(defn solve [instruction input]
  (let [outputs (-> input ic/parse-input (ic/add-input instruction) ic/run-to-completion ic/outputs)]
    (if (= 1 (count outputs))
      (first outputs)
      {:failing-opcodes outputs})))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 2 input))
```

---

## Refactoring #1: Run and Advance

Every time I create a new opcode instruction, I forget to advance the counter. It's not a lot of work, but a function
that adds values within the Intcode is inherently different from the process of moving the pointer. So, I decided to
refactor out a common function called `run-and-advance`, which takes in the actual function we want to run on the
Incode based on the opcode, executes it along with passing in the parameters for that opcode, and then moves the
instruction pointer based on the number of parameters.

```clojure
(defn- run-and-advance [f num-params int-code]
  (-> (f int-code (current-params int-code num-params))
      (advance-instruction-pointer (inc num-params))))
```

It's a simple function, but it lets us change all of our business functions to be simpler.  For example, here is the
before and after for 

```clojure
; Before refactoring.
(defn- arithmetic-instruction [f int-code]
  (let [[a b c] (current-params int-code 3)
        target-address (param-address-value int-code c)]
    (-> int-code
        (set-address-value target-address (f (param-value int-code a)
                                             (param-value int-code b)))
        (advance-instruction-pointer 4))))

(defmethod run-instruction 1 [int-code] (arithmetic-instruction + int-code))
(defmethod run-instruction 2 [int-code] (arithmetic-instruction * int-code))
        

; After refactoring
; Notice how much simpler the arithmetic-instruction code is, being a couple of `let` bindings and the single
; expression. The run-instruction multi-method is slightly trickier, in that it has to say that it wants to run the
; arithmetic instruction and then advance accordingly, but that seems more appropriate than keeping it in the
; arithmetic-instruction itself... I think.
(defn- arithmetic-instruction [f int-code params]
  (let [[a b c] params
        target-address (param-address-value int-code c)]
    (set-address-value int-code target-address (f (param-value int-code a)
                                                  (param-value int-code b)))))
(defmethod run-instruction 1 [int-code] (run-and-advance (partial arithmetic-instruction +) 3 int-code))
(defmethod run-instruction 2 [int-code] (run-and-advance (partial arithmetic-instruction *) 3 int-code))
```

We can then apply this to all of the instructions other than the jump instructions, since those don't always advance
the instruction pointer by a set amount.

```clojure
(defmethod run-instruction 1 [int-code] (run-and-advance (partial arithmetic-instruction +) 3 int-code))
(defmethod run-instruction 2 [int-code] (run-and-advance (partial arithmetic-instruction *) 3 int-code))
(defmethod run-instruction 3 [int-code] (run-and-advance input-instruction 1 int-code))
(defmethod run-instruction 4 [int-code] (run-and-advance output-instruction 1 int-code))
(defmethod run-instruction 5 [int-code] (jump-if-instruction (complement zero?) int-code))
(defmethod run-instruction 6 [int-code] (jump-if-instruction zero? int-code))
(defmethod run-instruction 7 [int-code] (run-and-advance (partial compare-instruction <) 3 int-code))
(defmethod run-instruction 8 [int-code] (run-and-advance (partial compare-instruction =) 3 int-code))
(defmethod run-instruction 9 [int-code] (run-and-advance adjust-relative-base 1 int-code))
```

---

## Refactoring #2: Naming Convention

It occurred to me that several of the functions in the Intcode are inherently impure, namely, those having to do with
the input and output channels. As a result, I decided to adopt the convention of naming these functions with a trailing
exclamation mark. This is mostly a marker to help when each day's code calls an impure function, rather than just
helping the logic within Intcode itself.

The new public function names with exclamation marks are:

* `add-input!`
* `add-output!`
* `outputs!` (because it pulls data out of the copy channel the one and only time it's possible)
* `chain-outputs-to!`

The new private function names with exclamation marks are:
* `input-instruction!`
* `output-instruction!`
