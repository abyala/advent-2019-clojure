# Day 8: Space Image Format

* [Problem Statement](https://adventofcode.com/2019/day/8)
* [Solution code](https://github.com/abyala/advent-2019-clojure/blob/main/src/advent_2019_clojure/day08.clj)

---

This was not the most interesting of problems, and I didn't see much reason to make it pretty or reusable. 

## Part 1

Given a long numeric string and a static `width` of 25 and `height` of 6, we have to find which layer (of size
`width*height`) has the fewest zeros, and then multiply its number of 1s by its number of 2s. Since we'll get a tiny
bit of reuse from part 2, we'll start with `parse-layers`, which returns a sequence of character sequences, each one
being a single layer.

```clojure
(def width 25)
(def height 6)

(defn parse-layers [input]
  (partition (* width height) input))
```

Then we're going to do everything for `part1` in a single pipelined function. First, we'll map each layer using
`frequencies`, which will return a sequence of maps of each character to its instance count within the layer. The
`sort-by` function will order the layers by the fewest number of zeros, using a default value of `0` if there are none.
Then to keep everything in a single expression, we'll map each of these ordered layers using a function that multiplies
the instances of `1`s and `2`s, and then call `first` to ensure we only call that mapping function once.

```clojure
(defn part1 [input]
  (->> (parse-layers input)
       (map frequencies)
       (sort-by #(get % \0 0) <)
       (map #(* (get % \1 0) (get % \2 0)))
       first))
```

I'm not overly proud of this, but it gets the job done.

---

## Part 2

This time, we look at all each pixel in order, finding the first layer to have a non-transparent value.  Then we print
out the output to see the secret message.  Again, we'll do this in a single function.

The initial goal is to make a sequence of character sequences, this time representing all layers for a given pixel;
this contrasts the data structure in part 1 where each sequence represented all pixels for a given layer. To do this,
after we parse the layers, we'll call `interleave` to pick a character from each layer at a time, and then call
`partition` again to create the pixel sequences. For each one, `visible-pixel` will pick out the `\0` or `\1`
character, making it printable by having the black characters show up as hash marks and white characters as spaces.

At that point, we have a long sequence of pixels, so we need to make it printable. We'll again call `partition`, this
time on the width of the image, which give us a sequence of character sequences, each one representing a line. We map
each sequence to a string by calling`(apply str)`, and then call the impure `(run! println coll)` to print the value
to the screen.

```clojure
(defn visible-pixel [pixels]
  (first (keep {\0 \space \1 \#} pixels)))

(defn part2 [input]
  (let [layers (parse-layers input)]
    (->> (apply interleave layers)
         (partition (count layers))
         (map visible-pixel)
         (partition width)
         (map (partial apply str))
         (run! println))))
```