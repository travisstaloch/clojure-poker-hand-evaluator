(ns poker-hand-evaluator.core
  (:use [poker-hand-evaluator.lookup-tables]
        [poker-hand-evaluator.lookup-tables-bin-search]
        [clojure.math.combinatorics]
        [clojure.stacktrace]
        ;;[clj-stacktrace.core]
        ))


(comment "
- By Travis Staloch
- Adapted from clojure-poker-hand-evaluator
https://github.com/s4nchez/clojure-poker-hand-evaluator
which is based on Cactus Kev's poker hand evaluator written in C
http://suffe.cool/poker/evaluator.html.
- Forked by Travis Staloch in order to add perfect hashing instead of binary search
for hands which aren't flushes, straights and high card.
- Adapted java perfect hashing implementation from
https://github.com/baleful/Poker-Hand-Evaluator.
- Noted small performance gain over binary search but not sure of significance.
  -- After adding test to sample one million random hands, perfect hashing is
  7x faster on average!!
  -- Profiling with tufte shows slightly more than 50x faster!!!
  -- Doh! I believe these figures were wrong and have decided real difference is ~50% faster
  -- AH HA! These LAZY sequences weren't actually being evaluated which was throwing off my
  benchmarks.  I believe perfect hashing is actually 2x to 3x faster than binary search
- Attempted to eliminate repeated use of '& 0xffffffff' in fast-find-hash-adjust
function but couldn't get it working using java.math.BigInteger bit operations.
Not sure what the problem is but left attempt in as fast-find-hash-adjust2.
Hoping for better performance.
  -- Using unsigned-bit-shift-right was able to  eliminate several '& 0xffffffff'
- Modified cards representation to include upper and lower case faces.
So the ace of spades can be 'AS' 'as' 'As' or 'aS' in addition to 'A♠'
- Added omaha evaluation which uses exactly 2/4 hold cards and 3/5 board cards
- Changed lookup tables from PersistentVectors to ArrayLists to improve lookup times.
- Added speed tests for 5, 7 and 9 card hands.  9 card doesn't use omaha evaluation
- Replaced some uses of apply and #() lambdas with .applyTo and local function
not sure if performance improved.
- Added low and hi-low evaluations.
- Added exception catching in a few places where some funny errors were happening
during benchmarking.
- Finally tracked down error caused by prep-samples in testing which was trying to
separate 9 cards into board and pockets seqs.  Just took this out.  Not performance
testing omaha.
- Changed from ArrayList lookup tables to int-array for slight performance gain.
Had to include custom binary-search implementation which uses aget.

TODO:
- Add tests for hi-low evaluations
- remove binary search lookup tables and code or figure out how to exclude ns in project file
or firgure out if clojure compiler does dead code elimination for us (dont think so)

")

(comment (def profiles (->> "project.clj" slurp read-string (drop 3) (partition 2) (map vec) (into {})
                  :profiles)))

(defn- log-exception [e & info]
  (printf "Caught exception: %s %s Info: %s %n%n" (.getMessage e)
    (with-out-str (print-stack-trace e 25))
    ;;(clojure.string/join ", " (map str info))
    (with-out-str (pr info))
    ))


(def suit-details
  "Available suits and the respective bit pattern to be used in the card format"
  {"♠" 0x1000, "♥" 0x2000, "♦" 0x4000, "♣" 0x8000})
(def suit-details-character-uppercase
  {"S"  0x1000, "H" 0x2000, "D" 0x4000, "C" 0x8000})
(def suit-details-character-lowercase
  {"s"  0x1000, "h" 0x2000, "d" 0x4000, "c" 0x8000})
(def suit-details-all
  (merge suit-details
         suit-details-character-uppercase
         suit-details-character-lowercase))

(def face-details-base
  "Available face values, including their assigned prime and their rank to be used in the card format"
  {"2" {:prime 2, :index 0}
   "3" {:prime 3, :index 1}
   "4" {:prime 5, :index 2}
   "5" {:prime 7, :index 3}
   "6" {:prime 11, :index 4}
   "7" {:prime 13, :index 5}
   "8" {:prime 17, :index 6}
   "9" {:prime 19, :index 7}}
   )

(def face-details
  (merge face-details-base
    {"T" {:prime 23, :index 8}
     "J" {:prime 29, :index 9}
     "Q" {:prime 31, :index 10}
     "K" {:prime 37, :index 11}
     "A" {:prime 41, :index 12}}))

(def face-details-lowercase
  (merge face-details-base
    {"t" {:prime 23, :index 8}
     "j" {:prime 29, :index 9}
     "q" {:prime 31, :index 10}
     "k" {:prime 37, :index 11}
     "a" {:prime 41, :index 12}}))

(def face-details-all
  (merge face-details
         face-details-lowercase))

(defn- card-value
  "Card representation as an integer, based on Kevin Suffecool's specs:
    +--------+--------+--------+--------+
    |xxxbbbbb|bbbbbbbb|cdhsrrrr|xxpppppp|
    +--------+--------+--------+--------+
    p = prime number of rank (deuce=2,trey=3,four=5,five=7,...,ace=41)
    r = rank of card (deuce=0,trey=1,four=2,five=3,...,ace=12)
    cdhs = suit of card
    b = bit turned on depending on rank of card"
  [face suit]
  (let [details (face-details-all face)
        prime (details :prime)
        face-value (details :index)
        suit-value (suit-details-all suit)]
    (bit-or prime (bit-shift-left face-value 8) suit-value (bit-shift-left 1 (+ 16 face-value)))))

(defn- generate-deck
  "creates map: card name -> card value"
  ([suit-dets face-dets]
  (let [deck {}]
    (into {} (for [face (keys face-dets)
                   suit (keys suit-dets)]
                [(str face suit) (card-value face suit)]))))
  ([]
    (generate-deck suit-details face-details) ))

(def deck
  "The default deck to be used by the evaluator"
  (generate-deck))

(def deck2
  "Multi deck allowing upper/lowercase faces and suits as is 'As' for ace of spades"
  (merge (generate-deck suit-details face-details)
         (generate-deck suit-details-character-uppercase face-details)
         (generate-deck suit-details-character-lowercase face-details)
         (generate-deck suit-details-character-uppercase face-details-lowercase)
         (generate-deck suit-details-character-lowercase face-details-lowercase) ))

(defn- calculate-hand-index
  "The hand index is calculated using:
  (c1 OR c2 OR c3 OR c4 OR c5) >> 16
  This value can be used later to find values in lookup tables."
  [cards] ;;(try
    (if (and (seq? cards) (not (nil? (first cards))) (= java.lang.Long (type (first cards))))
      (bit-shift-right (reduce bit-or cards) 16)
      (throw  (Exception. (str "cards must be seq of Longs. Received: seq?: "
        (seq? cards) ". type first: " (type (first cards)) ".  cards: "
        (with-out-str (pr cards))))))

          ;;  (catch Exception e
          ;;    (log-exception e :calculate-hand-index :cards (with-out-str (pr cards))) 0)
            )

(defn- flush-hand
  "The following expression is used to check if the hand is a flush:
      c1 AND c2 AND c3 AND c4 AND c5 AND 0xF000
   If the expression returns a non-zero value, then we have a flush and can use the lookup table for flushes
   to resolve the hand rank."
  [hand-index card-values]
  (and
    (not= (bit-and (.applyTo bit-and card-values) 0xF000) 0)
    (aget flush-to-rank hand-index)))

(defn- unique-card-hand
  "Straights or High Card hands are resolved using a specific lookup table to resolve hand with 5 unique cards.
  This lookup will return a hand rank only for straights and high cards (0 for any other hand)."
  [hand-index]
  (let [hand-rank (aget unique5-to-rank hand-index)]
    (and (not= hand-rank 0) hand-rank)))

(comment "
// from https://github.com/baleful/Poker-Hand-Evaluator Java perfect hash implementation
private static int find(long u) {
		// The repeated 0xffffffffL is to deal with Java's lack of unsigned int
		int a, b;
		int r;
		u += 0xe91aaa35;
		u &= 0xffffffffL;
		u ^= (u >> 16);
		u &= 0xffffffffL;
		u += (u << 8);
		u &= 0xffffffffL;
		u ^= u >> 4;
		u &= 0xffffffffL;
		b = (int) ((u >> 8) & 0x1ff);
		a = (int) (((u + ((u << 2) & 0xffffffffL)) & 0xffffffffL) >> 19);
		r = (int) (a ^ HashAdjust.values()[b]);
		return r;
}

// from Paul Senzee's perfect hash in C
		u += 0xe91aaa35;
    u ^= u >> 16;
    u += u << 8;
    u ^= u >> 4;
    b  = (u >> 8) & 0x1ff;
    a  = (u + (u << 2)) >> 19;
    r  = a ^ hash_adjust[b];
	}")

(comment (defn fast-find-hash-adjust-working [card-values]
  (let [ u (-> card-values
             (+ 0xe91aaa35)
             (bit-and 0xffffffff))
         v (bit-xor (bit-shift-right u 16) u)
         w (-> v
             (+ (bit-shift-left v 8))
             (bit-and 0xffffffff))
         x (-> w
             (bit-xor (bit-shift-right w 4))
             (bit-and 0xffffffff))
         b (bit-and (bit-shift-right x 8) 0x1ff)
         a (bit-shift-right (bit-and (+ x (bit-and (bit-shift-left x 2) 0xffffffff)) 0xffffffff)  19)
         r (bit-xor a (aget hash-adjust b))]
         r)))

(defn fast-find-hash-adjust [card-values]
  (let [ u (-> card-values
             (+ 0xe91aaa35))
         v (bit-xor (unsigned-bit-shift-right u 16) u)
         w (-> v
             (+ (bit-shift-left v 8))
             (bit-and 0xffffffff))
         x (-> w
             (bit-xor (unsigned-bit-shift-right w 4)))
         b (bit-and (unsigned-bit-shift-right x 8) 0x1ff)
         a (unsigned-bit-shift-right
             (bit-and (+ x (bit-shift-left x 2)) 0xffffffff) 19)
         r (bit-xor a (aget hash-adjust b))]
    r))
;; (fast-find-hash-adjust3 94352849)  ;; should == 7649

(defn- other-hands2 [card-values]
  (let [fn1 (fn [cv] (bit-and cv 0xFF))
        q (reduce * (map fn1 card-values))
        q-index (fast-find-hash-adjust q)]
    (or (aget hash-values q-index) false)))

(defn- calculate-hand-rank
  "Uses the following strategies to find the hand rank, in order:
    1. bit masking + lookup table for flush hands
    2. bit masking + lookup table for hands with 5 unique cards
    3. prime multiplying + 2 lookup tables for the remaining hands"
  ([hand other-hands-fn]
  (let [card-values (map deck2 hand)
        hand-index (calculate-hand-index card-values)]
    (or
      (flush-hand hand-index, card-values)
      (unique-card-hand hand-index)
      (other-hands-fn card-values))))
  ([hand] (calculate-hand-rank hand other-hands2)))

(def ranks
  "Poker ranks and their respective maximum rank"
  {7462 :HighCard
   6185 :OnePair
   3325 :TwoPairs
   2467 :ThreeOfAKind
   1609 :Straight
   1599 :Flush
   322 :FullHouse
   166 :FourOfAKind
   10 :StraightFlush})

(defn- resolve-rank-name
  "Resolves the name of a given rank"
  [hand-rank]
  (ranks (+ hand-rank (first (filter #(>= % 0) (sort (map #(- % hand-rank) (keys ranks))))))))

(defn- evaluate-hand
  "Evaluates a 5-card poker hand, returning a map including its name and rank"
  [& hand]
  (let [hand-rank (calculate-hand-rank hand)
        rank-name (resolve-rank-name hand-rank)]
    {:cards hand :rank hand-rank :hand rank-name}))

(defn- *est-rank [rank-fn evaluated-hands]
  (first (sort #(rank-fn (%1 :rank) (%2 :rank)) evaluated-hands)))

(defn- highest-rank
  "Finds the highest rank for a list of evaluated hands"
  [evaluated-hands] (*est-rank < evaluated-hands))

(defn- lowest-rank
  "Finds the lowest rank for a list of evaluated hands"
  [evaluated-hands] (*est-rank > evaluated-hands))

(defn- hi-and-lowest-ranks [rank-fn evaluated-hands]
  "Finds the highest and lowest rank for a list of evaluated hands"
  (let [sorted-hands (sort #(rank-fn (%1 :rank) (%2 :rank)) evaluated-hands)]
    (list (first sorted-hands) (last sorted-hands))))

(defn- evaluate-all-combinations
  "Evaluates all possible 5-card combinations for a hand"
  [hand] (let [evfn (fn [h] (.applyTo evaluate-hand h)) ] (map evfn (combinations hand 5))))

(defn evaluate
  "Evaluates a poker hand. If it contains more than 5 cards, it returns the best hand possible"
  [& hand] (highest-rank (evaluate-all-combinations hand)))

(defn evaluate-lowest [& hand] (lowest-rank (evaluate-all-combinations hand)))

(defn evaluate-hi-low [& hand] (hi-and-lowest-ranks (evaluate-all-combinations hand)))

(defn- ev-omaha
  "Evaluates an omaha hand. It returns the best hand possible for all combinations of 2 pocket + 3 board hands"
  ([board pockets eval-fn rank-fn]
    (let [evfn (fn [h] (.applyTo eval-fn h))]
      (rank-fn (map evfn
        (for [two-pock (combinations pockets 2)
              three-board (combinations board 3)] (concat two-pock three-board))))))
  ([board pockets eval-fn] (ev-omaha board pockets eval-fn highest-rank))
  ([board pockets] (ev-omaha board pockets evaluate highest-rank)))

(defn evaluate-omaha ([board pockets eval-fn]
  (ev-omaha board pockets eval-fn))
  ([board pockets] (evaluate-omaha board pockets evaluate)))

(defn split-hand
  "when given hand as one string (ie askcad2c5h) split into list of cards"
  [hand] (map #(clojure.string/join %) (partition 2 hand)))

(defn binary-search
  "Finds earliest occurrence of x in xs (a vector) using binary search."
  ([xs x]
   (loop [l 0 h (unchecked-dec (count xs))]
     (if (<= h (inc l))
       (cond
         (== x (aget xs l)) l
         (== x (aget xs h)) h
         :else nil)
       (let [m (unchecked-add l (bit-shift-right (unchecked-subtract h l) 1))]
         (if (< (aget xs m) x)
           (recur (unchecked-inc m) h)
           (recur l m)))))))

(defn- other-hands-bin-search
  "Other hands are all non-flush and non-unique5. We first calculate the prime product of all cards:
  q = (c1 AND 0xFF) * (c2 AND 0xFF) * ... * (c5 AND 0xFF)
  Because the range of q is huge (48-100M+), we use 2 lookup tables: we search the index of q on the first
  and then use this index on the second to find the actual hand rank."
  [card-values]
  (let [bit-and-fn  (fn [n] (bit-and n 0xFF))
        q (reduce * (map bit-and-fn card-values))
        q-index (binary-search prime-product-to-combination q)
        ;;q-index (java.util.Collections/binarySearch prime-product-to-combination q)
        ]
    (or (aget combination-to-rank q-index) false)))

(defn- evaluate-hand-bin-search
  "Evaluates a 5-card poker hand, returning a map including its name and rank using binary search "
  [& hand]
  (let [hand-rank (calculate-hand-rank hand other-hands-bin-search)
        rank-name (resolve-rank-name hand-rank)]
    {:cards hand :rank hand-rank :hand rank-name}))

(defn- evaluate-all-combinations-b
  "Evaluates all possible 5-card combinations for a hand"
  [hand]
  (let [evfn (fn [h] (.applyTo evaluate-hand-bin-search h))]
    (map evfn (combinations hand 5))))

(defn evaluate-b
  "Evaluates a poker hand. If it contains more than 5 cards, it returns the best hand possible"
  [& hand] (highest-rank (evaluate-all-combinations-b hand)))
