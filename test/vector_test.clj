(ns vector-test
  (:require [midje.sweet :refer :all]
            [spacewar.vector :as v]
            [midje.experimental :refer [for-all]]
            [clojure.spec.alpha :as s]))

(facts
  "about vectors"
  (fact
    "vector add"
    (for-all
      [[x1 y1 :as v1] (s/gen ::v/vector)
       [x2 y2 :as v2] (s/gen ::v/vector)]
      (let [sum (v/add v1 v2)]
        sum => #(s/valid? ::v/vector %))))

  (fact
    "vector add values"
    (v/add [0 0] [1 1]) => [1 1]
    (v/add [1 1] [1 1]) => [2 2]
    )

  (fact
    "vector scale properties"
    (for-all
      [v (s/gen ::v/vector)
       n (s/gen ::v/number)]
      (let [scaled (n v/scale v)]
        scaled => #(s/valid? ::v/vector %))))

  (fact
    "vector scale values"
    (v/scale 10 [1 1]) => [10 10])

  (fact
    "Magnitude properties"
    (for-all
      [v (s/gen ::v/vector)]
      (v/magnitude v) => #(s/valid? ::v/number %)))

  (fact
    "Magnitude values"
    (v/magnitude [0 0]) => (roughly 0)
    (v/magnitude [0 1]) => (roughly 1)
    (v/magnitude [1 0]) => (roughly 1)
    (v/magnitude [-1 -1]) => (roughly (Math/sqrt 2)))

  (fact
    "from-angular properties"
    (for-all
      [angle (s/gen ::v/number)
       length (s/gen ::v/number)]
      (v/from-angular length angle) => #(s/valid? ::v/vector %)))

  )
