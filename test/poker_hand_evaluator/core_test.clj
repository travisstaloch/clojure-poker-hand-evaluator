(ns poker-hand-evaluator.core-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [poker-hand-evaluator.core :refer :all]
            [environ.core :refer [env] ]
            ))

(set! *warn-on-reflection* true)

(defn- base2-str [x] (Integer/toString (int x) 2))

(defn hand
  [rank hand-name result]
  (and (= rank (result :rank)) (= hand-name (result :hand))))

(defn hand-and-cards
  [rank hand-name best-cards result]
  (and (hand rank hand-name result) (= best-cards (result :cards))))

(def high-seven                   ["2H" "3S" "4C" "5C" "7D"])
(def high-ace                     ["AH" "JS" "QC" "KC" "9D"])
(def pair-hand                    ["2H" "2S" "4C" "5C" "7D"])
(def pair-hand2                   ["KH" "KS" "4C" "5C" "7D"])
(def pair-hand3                   ["2H" "2S" "AC" "KC" "QD"])
(def pair-hand4                   ["3H" "3S" "4C" "5C" "6D"])
(def pair-hand5                   ["4C" "6C" "6H" "JS" "KC"])
(def pair-hand6                   ["3H" "4H" "7C" "7D" "8H"])
(def pair-hand7                   ["AH" "AS" "KC" "QC" "JD"])
(def two-pairs-hand               ["2H" "2S" "3C" "3D" "4D"])
(def two-pairs-hand2              ["2H" "2S" "4C" "4D" "AD"])
(def two-pairs-hand3              ["AH" "AS" "QC" "QD" "KD"])
(def three-of-a-kind-hand         ["2H" "2S" "2C" "4D" "5D"])
(def three-of-a-kind-hand2        ["TH" "TS" "TC" "4D" "7D"])
(def three-of-a-kind-hand3        ["AH" "AS" "AC" "KD" "QD"])
(def four-of-a-kind-hand          ["2H" "2S" "2C" "2D" "3D"])
(def four-of-a-kind-hand2         ["9H" "9S" "9C" "9D" "7D"])
(def straight-hand                ["2H" "3S" "6C" "5D" "4D"])
(def straight-hand-wheel          ["2H" "3S" "5C" "4D" "AD"])
(def straight-hand-broadway       ["KH" "QS" "TC" "JD" "AD"])
(def low-ace-straight-hand        ["2H" "3S" "4C" "5D" "AD"])
(def high-ace-straight-hand       ["TH" "AS" "QC" "KD" "JD"])
(def flush-hand                   ["2H" "4H" "5H" "9H" "7H"])
(def flush-hand-a-hi              ["2H" "4H" "5H" "9H" "AH"])
(def flush-hand-k-hi              ["2H" "4H" "5H" "9H" "KH"])
(def full-house-hand              ["2H" "5D" "2D" "2C" "5S"])
(def full-house-hand2             ["8H" "7D" "8D" "8C" "7S"])
(def straight-flush-hand          ["2H" "3H" "6H" "5H" "4H"])
(def straight-flush-steel-wheel   ["2D" "3D" "4D" "5D" "AD"])
(def straight-flush-royal         ["TS" "AS" "QS" "KS" "JS"])
(def holdem-board1                "ahjsqc2c3c")
(def holdem-pocket1               "kctc")
(def holdem-pocket2               "asqd")
(def holdem-pocket3               "adjc9d9h")
(def holdem-pocket4               "astc9d9h")
(def omaha-pocket1                "actc9d9h")
(def omaha-pocket2                "kctc9d9h")
(def omaha-pocket3                "khts9d9h")
(def omaha-pocket4                "acqh9d9h")
(def holdem-board2                "ahjcqc2c3c")
(def holdem-board3                "acjcqc2c3c")
(def omaha-pocket5                "kcqh9d9h")
(def holdem-board4                "acad9h9d9s")
(def omaha-pocket6                "ahjcqdth")

(comment (defmacro fn-name
  [f] ;;(prn :fn-name :f f)
  `(-> ~(if (symbol? f) f (first f)) var meta :name str) ))

(defn rk5 ([hand eval-fn]
  (let [[c1 c2 c3 c4 c5 c6 c7 c8 c9] hand
        r (eval-fn c1 c2 c3 c4 c5)]
    (:rank r)))
  ([hand] (rk5 hand evaluate)))

(defn rk7 ([hand eval-fn]
  (let [[c1 c2 c3 c4 c5 c6 c7 c8 c9] hand
        r (eval-fn c1 c2 c3 c4 c5 c6 c7)]
    (:rank r)))
  ([hand] (rk7 hand evaluate)))

(defn ev-o ([[p b] eval-fn] (evaluate-omaha p b eval-fn))
  ([[p b]] (ev-o [p b] evaluate)))

(defn rk9 ([[p b] eval-fn] (:rank (ev-o [p b] eval-fn)))
  ([[p b]] (rk9 [p b] evaluate)))

(defn hd9 ([[p b] eval-fn] (:hand (ev-o [p b] eval-fn)))
  ([[p b]] (hd9 [p b] evaluate)))

(defn combine-card-str [& coll]
  (split-hand (clojure.string/join coll)))

(defn combine-card-str2 [board pockets]
  [(split-hand (clojure.string/join board)) (split-hand (clojure.string/join pockets))])



(time [ (deftest deck-basics
  (comment (testing "Contains 52 cards"
    (is (= 52 (count deck)))))
  (testing "deck2 contains 196 cards lookups adding upper and lowercase suits and faces to standard deck"
    (is (= 216 (count deck2))))
  (comment (testing "Cards are standard"
    (is (= (set (for [s '("♣" "♥" "♠" "♦") f '("2" "3" "4" "5" "6" "7" "8" "9" "T" "J" "Q" "K" "A")] (str f s)))
          (set (keys deck))))))
  (testing "K♦ is represented with correct bit pattern"
    (is (= "1000000000000100101100100101" (base2-str (deck2 "K♦")))))
  (testing "5♠ is represented with correct bit pattern"
    (is (= "10000001001100000111" (base2-str (deck2 "5♠")))))
  (testing "J♣ is represented with correct bit pattern"
    (is (= "10000000001000100100011101" (base2-str (deck2 "J♣")))))
  (testing "kd is represented with correct bit pattern"
    (is (= "1000000000000100101100100101" (base2-str (deck2 "kd")))))
  (testing "5S is represented with correct bit pattern"
    (is (= "10000001001100000111" (base2-str (deck2 "5S")))))
  (testing "jc is represented with correct bit pattern"
    (is (= "10000000001000100100011101" (base2-str (deck2 "jc")))))
  (testing "Jc is represented with correct bit pattern"
    (is (= "10000000001000100100011101" (base2-str (deck2 "Jc")))))
  (testing "jC is represented with correct bit pattern"
    (is (= "10000000001000100100011101" (base2-str (deck2 "jC"))))))

(deftest evaluation
  (testing "Straight Flush"
    (is (hand  1 :StraightFlush (evaluate "T♣" "J♣" "Q♣" "K♣" "A♣")))
    (is (hand 10 :StraightFlush (evaluate "A♣" "2♣" "3♣" "4♣" "5♣"))))
  (testing "Four of a kind"
    (is (hand 11 :FourOfAKind (evaluate "K♣" "A♣" "A♥" "A♦" "A♣")))
    (is (hand 166 :FourOfAKind (evaluate "3♣" "2♣" "2♥" "2♦" "2♣"))))
  (testing "Full House"
    (is (hand 167 :FullHouse (evaluate "K♣" "K♥" "A♥" "A♦" "A♣")))
    (is (hand 322 :FullHouse (evaluate "3♣" "3♥" "2♥" "2♦" "2♣"))))
  (testing "Flush"
    (is (hand 323 :Flush (evaluate "9♣" "J♣" "Q♣" "K♣" "A♣")))
    (is (hand 1599 :Flush (evaluate "2♣" "3♣" "4♣" "5♣" "7♣"))))
  (testing "Straight"
    (is (hand 1600 :Straight (evaluate "T♣" "J♣" "Q♦" "K♥" "A♣")))
    (is (hand 1609 :Straight (evaluate "A♣" "2♣" "3♦" "4♥" "5♣"))))
  (testing "Three of a Kind"
    (is (hand 1610 :ThreeOfAKind (evaluate "Q♣" "K♥" "A♥" "A♦" "A♣")))
    (is (hand 2467 :ThreeOfAKind (evaluate "4♣" "3♥" "2♥" "2♦" "2♣"))))
  (testing "Two Pairs"
    (is (hand 2468 :TwoPairs (evaluate "Q♣" "K♣" "K♦" "A♥" "A♣")))
    (is (hand 3325 :TwoPairs (evaluate "4♣" "3♣" "3♦" "2♥" "2♣"))))
  (testing "One Pair"
    (is (hand 3326 :OnePair (evaluate "J♣" "Q♦" "K♦" "A♥" "A♣")))
    (is (hand 6185 :OnePair (evaluate "2♣" "2♦" "3♦" "4♥" "5♣"))))
  (testing "Highest Card"
    (is (hand 6186 :HighCard (evaluate "9♣" "J♦" "Q♦" "K♥" "A♣")))
    (is (hand 7462 :HighCard (evaluate "2♣" "3♦" "4♦" "5♥" "7♣")))))

(deftest evaluation-more-than-5-cards
  (testing "Finds highest straight out of 7 cards"
    (is (hand-and-cards 1600 :Straight '("T♣" "J♣" "Q♦" "K♥" "A♣")
          (evaluate "8♣" "9♦" "T♣" "J♣" "Q♦" "K♥" "A♣"))))
  (testing "Finds highest straight out of 7 cards in reverse order"
    (is (hand-and-cards 1600 :Straight '("A♣" "K♦" "Q♣" "J♣" "T♦")
          (evaluate "A♣" "K♦" "Q♣" "J♣" "T♦" "9♥" "8♣"))))
  (testing "Finds highest straight out of 6 cards"
    (is (hand-and-cards 1600 :Straight '("T♣" "J♣" "Q♦" "K♥" "A♣")
          (evaluate "9♦" "T♣" "J♣" "Q♦" "K♥" "A♣"))))
  )

(deftest test-find-fast
  (testing "fast-find-hash-adjust perfect hashing calcs and lookups working"
    (is (= (fast-find-hash-adjust 94352849) 7649))
    (is (= (fast-find-hash-adjust 104553157) 7893))))

(deftest hand-rank5-test
  (testing "hand-rank5"
    (is (> (rk5 high-seven) (rk5 high-ace)))
    (is (> (rk5 high-ace) (rk5 pair-hand)))
    (is (> (rk5 pair-hand) (rk5 pair-hand2)))
    (is (> (rk5 pair-hand3) (rk5 pair-hand4)))
    (is (> (rk5 pair-hand5) (rk5 pair-hand6)))
    (is (> (rk5 pair-hand7) (rk5 two-pairs-hand)))
    (is (> (rk5 two-pairs-hand) (rk5 two-pairs-hand2)))
    (is (> (rk5 two-pairs-hand2) (rk5 two-pairs-hand3)))
    (is (> (rk5 two-pairs-hand3) (rk5 three-of-a-kind-hand)))
    (is (> (rk5 three-of-a-kind-hand) (rk5 three-of-a-kind-hand2)))
    (is (> (rk5 three-of-a-kind-hand2) (rk5 three-of-a-kind-hand3)))
    (is (> (rk5 straight-hand-wheel) (rk5 straight-hand)))
    (is (> (rk5 straight-hand) (rk5 straight-hand-broadway)))
    (is (> (rk5 straight-hand-broadway) (rk5 flush-hand)))
    (is (> (rk5 flush-hand) (rk5 flush-hand-k-hi)))
    (is (> (rk5 flush-hand-k-hi) (rk5 flush-hand-a-hi)))
    (is (> (rk5 flush-hand-a-hi) (rk5 full-house-hand)))
    (is (> (rk5 full-house-hand) (rk5 full-house-hand2)))
    (is (> (rk5 full-house-hand2) (rk5 four-of-a-kind-hand)))
    (is (> (rk5 four-of-a-kind-hand) (rk5 four-of-a-kind-hand2)))
    (is (> (rk5 four-of-a-kind-hand2) (rk5 straight-flush-steel-wheel)))
    (is (> (rk5 straight-flush-steel-wheel) (rk5 straight-flush-hand)))
    (is (> (rk5 straight-flush-hand) (rk5 straight-flush-royal)))
   ))

(deftest holdem-specific
 (testing "holdem specific close 2 pr"
    (is (> (rk7 (combine-card-str holdem-board1 holdem-pocket4))
           (rk7 (combine-card-str holdem-board1 holdem-pocket3))))
    (is (> (rk7 (combine-card-str holdem-board1 holdem-pocket3))
           (rk7 (combine-card-str holdem-board1 holdem-pocket2))))
    (is (> (rk7 (combine-card-str holdem-board1 holdem-pocket2))
           (rk7 (combine-card-str holdem-board1 holdem-pocket1))))
  ))

(deftest omaha-specific
  (testing "omaha specific tests"
    (is (> (rk9 (combine-card-str2 holdem-board1 omaha-pocket4))
           (rk9 (combine-card-str2 holdem-board1 omaha-pocket3))))
    (is (> (rk9 (combine-card-str2 holdem-board1 omaha-pocket3))
           (rk9 (combine-card-str2 holdem-board1 omaha-pocket2))))
    (is (> (rk9 (combine-card-str2 holdem-board1 omaha-pocket2))
           (rk9 (combine-card-str2 holdem-board1 omaha-pocket1))))
    (is (> (rk9 (combine-card-str2 holdem-board2 omaha-pocket4))
           (rk9 (combine-card-str2 holdem-board2 omaha-pocket1))))
    (is (= :TwoPairs (hd9 (combine-card-str2 holdem-board2 omaha-pocket4))))
    (is (= :OnePair  (hd9 (combine-card-str2 holdem-board3 omaha-pocket5))))
    (is (= :ThreeOfAKind (hd9 (combine-card-str2 holdem-board4 omaha-pocket6))))
           ))

(deftest low-eval
  (testing "low-eval"
           (is (= (evaluate-low "2c" "2d" "3h" "3d" "4s" "5c" "7h")
              { :rank 7462, :hand :HighCard, :cards '("2c" "3h" "4s" "5c" "7h")}))
))

(deftest hi-low-eval
  (testing "hi-low-eval"
     (is (= (evaluate-hi-low "2c" "2d" "3h" "3d" "4s" "5c" "7h")
            {:hi {:cards '("2c" "2d" "3h" "3d" "7h"), :rank 3322, :hand :TwoPairs},
            :low {:cards '("2d" "3d" "4s" "5c" "7h"), :rank 7462, :hand :HighCard}}
            ))
     ))

(deftest omaha-hi-low-eval
  (testing "omaha-hi-low-eval"
     (is (= (evaluate-omaha-hi-low ["2c" "3d" "4s" "5c" "7h"] ["2d" "3h" "8d" "9h"])
            {:hi {:cards '("2d" "3h" "2c" "3d" "7h"), :rank 3322, :hand :TwoPairs},
             :low {:cards '("2d" "3h" "4s" "5c" "7h"), :rank 7462, :hand :HighCard}}
            ))
     ))
])


(if (env :performance-test) (do

(defn random-hand-ncards [n]
  (gen/vector-distinct
   (gen/elements (keys deck))
   {:min-elements n :max-elements n}))

(defn speed-test-fn [title n-samples n-cards rk-fn]
  (let [samples (gen/sample (random-hand-ncards n-cards) n-samples)
        ms (fn [t] (/ t 1000000.0))
        start (. System (nanoTime))
        x (for [s samples] (rk-fn s evaluate-b))
        countx (count x) ;; lazy - make sure these are evaluated
        done1 (. System (nanoTime))
        start2 (. System (nanoTime))
        y (for [s samples] (rk-fn s))
        county (count y) ;; lazy - make sure these are evaluated
        done2 (. System (nanoTime))
        bin-time (ms (double (- done1 start)))
        hash-time (ms (double (- done2 start2)))]
    (prn :counts countx county)
    (printf  "%s - binary-search-time: %fms hash-time: %fms - thats %f times faster%n"
             title bin-time hash-time (/ bin-time hash-time))))

(def benchmark-runs 100)
(deftest speed-test1
  (testing "binary search vs perfect hashing performance comparison 5 card hands"
           (speed-test-fn "1. (5 cards)" benchmark-runs 5 rk5)
           (is true)))

(deftest speed-test2
  (testing "binary search vs perfect hashing performance comparison 7 card hands"
           (speed-test-fn "2. (7 cards)" benchmark-runs 7 rk7)
           (is true)))

(deftest speed-test3
  (testing "binary search vs perfect hashing performance comparison 9 card hands"
           (speed-test-fn "3. (9 cards)" benchmark-runs 9 rk7)
           (is true)))

(comment (deftest array-lookup-speed-test
  (testing "are int-array lookups faster than ArrayList?"
           (let [arrsize 10000001
                 intarr (int-array arrsize)
                 arrlist (new java.util.ArrayList (range arrsize))
                 time1 (with-out-str (time (dotimes [x arrsize] (aget intarr x))))
                 time2 (with-out-str (time (dotimes [x arrsize] (.get arrlist x))))
                 ]
             (printf "int-array lookup time: %s.  ArrayList lookup time: %s" time1 time2)
             ))
  ))))