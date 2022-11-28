(ns advent-2019-clojure.day18-test
  (:require [clojure.test :refer :all]
            [advent-2019-clojure.test-utils :as test-utils]
            [advent-2019-clojure.day18 :refer :all]))

(def puzzle-data (test-utils/slurp-puzzle-data *ns*))

(deftest part1-test
  (are [expected input-maze] (= expected (time (part1 input-maze)))
                             8 "#########\n#b.A.@.a#\n#########"
                             86 "########################\n#f.D.E.e.C.b.A.@.a.B.c.#\n######################.#\n#d.....................#\n########################"
                             132 "########################\n#...............b.C.D.f#\n#.######################\n#.....@.a.B.c.d.A.e.F.g#\n########################"
                             136 "#################\n#i.G..c...e..H.p#\n########.########\n#j.A..b...f..D.o#\n########@########\n#k.E..a...g..B.n#\n########.########\n#l.F..d...h..C.m#\n#################"
                             81 "########################\n#@..............ac.GI.b#\n###d#e#f################\n###A#B#C################\n###g#h#i################\n########################"
                             5450 puzzle-data))

#_(deftest part2-test
  (are [expected input] (= expected (part2 input))
                        "84462026" "03036732577212944063491565474664"
                        "78725270" "02935109699940807407585447034323"
                        "53553731" "03081770884921959731165446850517"
                        "92768399" puzzle-data))
