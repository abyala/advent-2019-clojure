# Day 7: Amplification Circuit

* [Problem Statement](https://adventofcode.com/2019/day/7)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day07.clj)

---

## Somewhat Big Refactoring

In today's problem, we're going to eventually get to a state where we will want to run multiple instances of Intcode
simultaneously, such that the output of one Intcode machine needs to hook up to the input of another machine. So before
we delve into the problem itself, let's add some asynchronous capabilities to the implementation. For this, we will
leverage Clojure's [core.async](https://clojure.github.io/core.async/) namespace.

For now, I'll assume that we mostly want to use blocking operations, as if Intcode 2 is trying to read an input that
Intcode 1 has yet to output, Intcode 2 should wait. Therefore, with few exceptions, I'll use the functions `<!!` and
`>!!` instead of `<!` and `>!`.

### Intcode structure

To start off, the overall structure of an Intcode is changing, removing the `:inputs` and `:outputs` properties with
`:input` and `:output`. The former is a channel of incoming values, and the latter is a map that contains the output
channel, an async [mult](https://clojure.github.io/core.async/#clojure.core.async/mult), and an internal-only 
"copy channel" that holds a copy of all data being sent as output. We'll also immediately
[tap](https://clojure.github.io/core.async/#clojure.core.async/tap) the `mult` into the copy channel. As these channels
need to buffer instead of rendevous, we arbitrarily set the buffers of both the input channel and the output copy
channel to 1000.

```clojure
(defn parse-input [s]
  (let [out-chan (chan)
        out-mult (mult out-chan)
        out-copy-chan (chan 1000)]
    (tap out-mult out-copy-chan)
    {:instruction-pointer 0
     :addresses           (reduce-kv (fn [m k v] (assoc m k (parse-long v))) {} (str/split s #","))
     :input               (chan 1000)
     :output              {:channel   out-chan
                           :mult      out-mult
                           :copy-chan out-copy-chan}}))
```

### Inputting data

Next we'll move on to changes for inputting data. The `add-input` function changes to push the value into the `:input`
channel using the `>!!` function, again since blocking should be fine here. Going with that is a change to the
`input-instruction` function, which reads data out of that same channel using the `<!!` function. Note that we no
longer need the `drop-inputs` function at all, since reading data from the channel should remove that value from the
channel.

```clojure
(defn add-input [int-code v]
  (>!! (:input int-code) v)
  int-code)

(defn- input-instruction [int-code]
  (let [input (<!! (:input int-code))
        address (-> int-code (current-params 1) first param-immediate-value)]
    (-> int-code
        (set-address-value address input)
        (advance-instruction-pointer 2))))
```

### Outputting data

We'll make similar changes to outputting data. The `add-output` function will push the data into the output channel
at location `[:output :channel]`, which again will flow through the mult to the tap(s) we've set up. We'll also have
to change the `outputs` function, since that's suppose to dump out all outputs that the Intcode has sent. So far, it's
reasonable to assume that we'll only call this after the Intcode has stopped working and its channels have closed,
so we'll use `async/into` to pull all the output data from the channel into a vector. For now, we'll also assume we
will only call the `outputs` function once; if we need to call it more times, then we'll need to use an Atom to store
a cache of the output data, since channels are mutable but a local cache wouldn't necessarily be so. The alternative
is to pull all the data out of the copy channel whenever the Intcode halts, but I don't know if future problems will
necessarily assume the Intcode will halt at all.

The `output-instruction` logic doesn't change at all, since everything was abstracted away into `add-output`.

```clojure
(defn add-output [int-code v]
  (>!! (get-in int-code [:output :channel]) v)
  int-code)
(defn outputs [int-code]
  (<!! (async/into [] (get-in int-code [:output :copy-chan]))))
(defn- output-instruction [int-code]
  (let [output (param-value int-code (-> int-code (current-params 1) first))]
    (-> int-code
        (add-output output)
        (advance-instruction-pointer 2))))
```

## Intcode halting

The other change we're making is to let the Intcode halt more gracefully now, setting a `:halted?` flag when it
encounters opcode 99. This will allow us to close the channels and return the completed Intcode, instead of dealing
with a `nil` and side effects. Note that since `close!` only works within a `go` block, the `halt-instruction`
function spawns two `go` blocks too.  Also, `run-to-completion` will return the first iteration with a halted Intcode,
instead of returning the last non-nil value.

```clojure
(defn- halt-instruction [int-code]
  (go (close! (:input int-code)))
  (go (close! (get-in int-code [:output :channel])))
  (assoc int-code :halted? true))

(defn run-to-completion [int-code]
  (->> (iterate run-instruction int-code)
       (filter :halted?)
       first))
```

That was a lot of work to bring us back to where we started! But with all of that in place, Day 7 turns out not to be
too bad.

---

# Part 1

For today's problem, we need to chain Intcode machines together. There will be five machines, and the output of each
one becomes the input for the next one, where the first Intcode gets initialized with a zero. In the end, we'll need
to read the output value of Intcode 5.

With our latest changes, this isn't bad at all. First, we'll add a function called `chain-outputs-to` to the Intcode
namespace, which taps the output `mult` of one Intcode into the input of the next Intcode. This wiring becomes a simple
one-liner.

```clojure
(defn chain-outputs-to [output-int-code input-int-code]
  (tap (get-in output-int-code [:output :mult]) (:input input-int-code)))
```

Now we need to set up the amplifier series, and again because we're dealing with channels, which are mutable, this
won't look like the pure functions we're used to. The function will take in its input string and the initialization
phases. Each phase will be mapped to a newly parsed Intcode into which we send the phase. Then we'll send the initial
zero value to the first intcode. Next, we'll pair together every adjacent Intcode using `(partition 2 1 int-codes)`, 
and call `run!` to mutate the first four by passing their output into the next Intcode. Next, we'll again call
`run!` to spawn a future for each Intcode, which calls `run-to-completion`, thus letting them all run concurrently.
Finally, we'll take the last output of the last IntCode.

```clojure
(defn run-amplifier-series [input phases]
  (let [int-codes (mapv (fn [phase] (-> (ic/parse-input input)
                                        (ic/add-input phase)))
                        phases)]
    (ic/add-input (first int-codes) 0)
    (run! (fn [[outputter inputter]] (ic/chain-outputs-to outputter inputter))
          (partition 2 1 int-codes))
    (run! #(future (ic/run-to-completion %))
          int-codes)
    (->> int-codes last ic/outputs last)))
```

It's time to write our `part1` function. We need to try all permutations of phases from 0 to 4, and for this I found
Clojure's `math.combinatorics` package to be particularly useful, with its `permutations` function. Starting with the
range from 0 to 4, we find all permutations, for each one we call `run-amplifier-series` to get the last output, and
then we calculate the max value to get our answer.

```clojure
(defn part1 [input]
  (->> (combo/permutations (range 5))
       (map (partial run-amplifier-series input))
       (apply max)))
```

---

# Part 2

There's very little work to be done for part 2, as there are only two small changes from part 1. First of all, the 
amplifier series must become an amplifier loop, where the output of Intcode 5 feeds into the input of Intcode 1. So
besides renaming the function, the `run-amplifier-loop` function only differs in that the `partition` function now
adds the first Intcode to the end, thus allowing us to set up 5 chains instead of 4. Note that Part 1 can use this
code too, since the first Intcode will halt before it tries to read the output from the last Intcode, so we don't
need `run-amplifier-series` at all anymore.

```clojure
(defn run-amplifier-loop [input phases]
  (let [int-codes (mapv (fn [phase] (-> (ic/parse-input input)
                                       (ic/add-input phase)))
                       phases)]
    (ic/add-input (first int-codes) 0)
    (run! (fn [[outputter inputter]] (ic/chain-outputs-to outputter inputter))
          (partition 2 1 (conj int-codes (first int-codes))))
    (run! #(future (ic/run-to-completion %))
          int-codes)
    (->> int-codes last ic/outputs last)))
```

Then part 2 uses the phase values from 5 through 9 but otherwise follows the same algorithm as part 1 did, so we can
create a unified `solve` function that takes in the range of values over which it calculates the permutations. That
gives us a very simple solution when all is said and done.

```clojure
(defn solve [possible-phases input]
  (->> (combo/permutations possible-phases)
       (map (partial run-amplifier-loop input))
       (apply max)))

(defn part1 [input] (solve (range 5) input))
(defn part2 [input] (solve (range 5 10) input))
```
