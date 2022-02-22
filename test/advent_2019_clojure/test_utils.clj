(ns advent-2019-clojure.test-utils
  (:require [clojure.string :as str]))

(defn- slurp-file [ns suffix]
  (-> (str "resources/"
           (str/replace ns #".*\.(day\d+)-test" "$1")
           suffix)
      slurp
      (str/replace "\r\n" "\n")))

(defn slurp-puzzle-data [ns]
  (slurp-file ns "_puzzle.txt"))