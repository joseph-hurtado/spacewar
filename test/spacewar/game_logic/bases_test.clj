(ns spacewar.game-logic.bases-test
  (:require [midje.sweet :refer :all]
            [spacewar.game-logic.config :refer :all]
            [spacewar.game-logic.bases :refer :all]
            [spacewar.game-logic.test-mother :as mom]))

(fact
  "bases age"
  (let [bases [(make-random-base) (make-random-base)]
        aged-bases (age-bases 10 bases)]
    (:age (first aged-bases)) => 10
    (:age (second aged-bases)) => 10))

(fact
  "bases stop aging once mature"
  (let [base (make-random-base)
        base (assoc base :age base-maturity-age)
        bases [base]
        aged-bases (age-bases 10 bases)]
    (:age (first aged-bases)) => base-maturity-age))

(fact
  "Immature bases do not manufacture"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (dec base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (dec base-maturity-age)
                               :type :dilithium-factory)
        wpn-base (assoc wpn-base :age (dec base-maturity-age)
                                 :type :weapon-factory)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => 0
    (:dilithium dl-base) => 0
    (:torpedos wpn-base) => 0
    (:kinetics wpn-base) => 0))

(fact
  "Mature bases do manufacture"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (inc base-maturity-age)
                               :type :antimatter-factory)
        dl-base (assoc dl-base :age (inc base-maturity-age)
                               :type :dilithium-factory)
        wpn-base (assoc wpn-base :age (inc base-maturity-age)
                                 :type :weapon-factory)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => (* 10 antimatter-factory-production-rate)
    (:dilithium dl-base) => (* 10 dilithium-factory-production-rate)
    (:torpedos wpn-base) => (* 10 weapon-factory-torpedo-production-rate)
    (:kinetics wpn-base) => (* 10 weapon-factory-kinetic-production-rate)))

(fact
  "Mature bases do not manufacture beyond maximums"
  (let [am-base (mom/make-base)
        dl-base (mom/make-base)
        wpn-base (mom/make-base)
        am-base (assoc am-base :age (inc base-maturity-age)
                               :type :antimatter-factory
                               :antimatter base-antimatter-maximum)
        dl-base (assoc dl-base :age (inc base-maturity-age)
                               :type :dilithium-factory
                               :dilithium base-dilithium-maximum)
        wpn-base (assoc wpn-base :age (inc base-maturity-age)
                                 :type :weapon-factory
                                 :kinetics base-kinetics-maximum
                                 :torpedos base-torpedos-maximum)
        bases [am-base dl-base wpn-base]
        [am-base dl-base wpn-base] (update-bases-manufacturing 10 bases)]
    (:antimatter am-base) => base-antimatter-maximum
    (:dilithium dl-base) => base-dilithium-maximum
    (:torpedos wpn-base) => base-torpedos-maximum
    (:kinetics wpn-base) => base-kinetics-maximum))

(fact
  "no transport route to bases that are out of range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (inc trade-route-limit) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (inc trade-route-limit) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for am-base bases)]
    transport-routes => []))

(fact
  "find transport route to bases that are in range"
  (let [wpn-base (mom/make-base 0 0 :weapon-factory 0 0)
        dl-base (mom/make-base (dec trade-route-limit) 0 :dilithium-factory 0 0)
        am-base (mom/make-base 0 (dec trade-route-limit) :antimatter-factory 0 0 0 0)
        bases [am-base dl-base wpn-base]
        transport-routes (find-transport-targets-for wpn-base bases)]
    transport-routes => (just #{dl-base am-base})))

(fact
  "base cannot launch transport if transport is not ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness (dec transport-ready))]
    (transport-ready? base) => false))

(fact
  "base can launch transport if transport is ready"
  (let [base (mom/make-base 0 0 :antimatter-factory 0 0)
        base (assoc base :transport-readiness transport-ready)]
    (transport-ready? base) => true))

(fact
  "transport readiness increases with time"
  (let [base (mom/make-base)
        base (assoc base :transport-readiness 0)
        world (assoc (mom/make-world) :bases [base])
        {:keys [bases]} (update-bases 10 world)
        base (first bases)]
    (:transport-readiness base) => 10))

(fact
  "transport readiness does not increase beyond readiness"
  (let [base (mom/make-base)
        base (assoc base :transport-readiness transport-ready)
        base (update-transport-readiness-for 10 base)]
    (:transport-readiness base) => transport-ready))

(tabular
  (facts
  "Transport antimatter from source to destination "
  (let [source (mom/make-base 0 0 ?source-type 0 0)
        dest (mom/make-base 0 0 ?dest-type 0 0)]

    (fact
      "should not transport if destination has sufficient antimatter"
      (let [source (assoc source :antimatter (inc (+ ?reserve antimatter-cargo-size)))
            dest (assoc dest :antimatter (inc ?sufficient))]
        (should-transport-antimatter? source dest) => false))

    (fact
      "should not transport if source antimatter would go below reserve"
      (let [source (assoc source :antimatter (dec (+ ?reserve antimatter-cargo-size)))
            dest (assoc dest :antimatter (dec ?sufficient))]
        (should-transport-antimatter? source dest) => false))

    (fact
      "should transport if destination has insufficient antimatter and source will not go below reserve"
      (let [source (assoc source :antimatter (inc (+ ?reserve antimatter-cargo-size)))
            dest (assoc dest :antimatter (dec ?sufficient))]
        (should-transport-antimatter? source dest) => true))))
  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory antimatter-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :antimatter-factory :weapon-factory     antimatter-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :antimatter-factory :dilithium-factory  antimatter-factory-antimatter-reserve dilithium-factory-sufficient-antimatter

  :dilithium-factory :antimatter-factory dilithium-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :dilithium-factory :weapon-factory     dilithium-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :dilithium-factory :dilithium-factory  dilithium-factory-antimatter-reserve dilithium-factory-sufficient-antimatter

  :weapon-factory :antimatter-factory weapon-factory-antimatter-reserve antimatter-factory-sufficient-antimatter
  :weapon-factory :weapon-factory     weapon-factory-antimatter-reserve weapon-factory-sufficient-antimatter
  :weapon-factory :dilithium-factory  weapon-factory-antimatter-reserve dilithium-factory-sufficient-antimatter
  )

(tabular
  (facts
  "Transport dilithium from source to destination "
  (let [source (mom/make-base 0 0 ?source-type 0 0)
        dest (mom/make-base 0 0 ?dest-type 0 0)]

    (fact
      "should not transport if destination has sufficient antimatter"
      (let [source (assoc source :dilithium (inc (+ ?reserve dilithium-cargo-size)))
            dest (assoc dest :dilithium (inc ?sufficient))]
        (should-transport-dilithium? source dest) => false))

    (fact
      "should not transport if source antimatter would go below reserve"
      (let [source (assoc source :dilithium (dec (+ ?reserve dilithium-cargo-size)))
            dest (assoc dest :dilithium (dec ?sufficient))]
        (should-transport-dilithium? source dest) => false))

    (fact
      "should transport if destination has insufficient antimatter and source will not go below reserve"
      (let [source (assoc source :dilithium (inc (+ ?reserve dilithium-cargo-size)))
            dest (assoc dest :dilithium (dec ?sufficient))]
        (should-transport-dilithium? source dest) => true))))
  ?source-type ?dest-type ?reserve ?sufficient
  :antimatter-factory :antimatter-factory antimatter-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :antimatter-factory :weapon-factory     antimatter-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :antimatter-factory :dilithium-factory  antimatter-factory-dilithium-reserve dilithium-factory-sufficient-dilithium

  :dilithium-factory :antimatter-factory dilithium-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :dilithium-factory :weapon-factory     dilithium-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :dilithium-factory :dilithium-factory  dilithium-factory-dilithium-reserve dilithium-factory-sufficient-dilithium

  :weapon-factory :antimatter-factory weapon-factory-dilithium-reserve antimatter-factory-sufficient-dilithium
  :weapon-factory :weapon-factory     weapon-factory-dilithium-reserve weapon-factory-sufficient-dilithium
  :weapon-factory :dilithium-factory  weapon-factory-dilithium-reserve dilithium-factory-sufficient-dilithium
  )

