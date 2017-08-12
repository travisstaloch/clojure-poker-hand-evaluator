# poker-hand-evaluator

A Clojure library designed to evaluate poker hands.

## Usage

```clojure
(use 'poker-hand-evaluator.core)

(evaluate "T♣" "J♣" "Q♣" "K♣" "A♣")
(evaluate "TC" "JC" "QC" "KC" "AC")
(evaluate "tc" "jc" "qc" "kc" "ac")
(evaluate "Tc" "jC" "QC" "kc" "a♣")
;= {:rank 1, :hand :StraightFlush, :cards ("T♣" "J♣" "Q♣" "K♣" "A♣")}
;; :cards will contain symbols given
```

low evaluation:
```clojure
(evaluate-low "2c" "2d" "3h" "3d" "4s" "5c" "7h")
;= { :rank 7462, :hand :HighCard, :cards ("2c" "3h" "4s" "5c" "7h")}
```

basic support for 7-card evaluation:
```clojure
(evaluate "8♣" "9♦" "T♣" "J♣" "Q♦" "K♥" "A♣")
;= {:rank 1600 :hand :Straight :cards ("T♣" "J♣" "Q♦" "K♥" "A♣")}
```

omaha evaluation:
```clojure
(evaluate-omaha ["ac" "ad" "9h" "9d" "9s"] ["ah" "jc" "qd" "th"]) ;; [board pockets]
;= {:rank 1623, :hand :ThreeOfAKind, :cards ("ah" "qd" "ac" "ad" "9h")}
```

hi-low evaluation:
```clojure
(evaluate-hi-low "2c" "2d" "3h" "3d" "4s" "5c" "7h")
;= {:hi {:rank 3322, :hand :TwoPairs, :cards ("2c" "2d" "3h" "3d" "7h")},
;;  :low {:rank 7462, :hand :HighCard, :cards ("2d" "3d" "4s" "5c" "7h") }}
```

omaha-hi-low evaluation:
```clojure
(evaluate-omaha-hi-low ["2c" "3d" "4s" "5c" "7h"] ["2d" "3h" "8d" "9h"])
;= {:hi {:rank 3322, :hand :TwoPairs, :cards '("2d" "3h" "2c" "3d" "7h")},
;;  :low {:rank 7462, :hand :HighCard, :cards '("2d" "3h" "4s" "5c" "7h")}}
```


## Running tests

To run all tests once:

```bash
lein test
```

During development, use [quickie](https://github.com/jakepearson/quickie) to rerun tests automatically when files change:

```bash
lein quickie
```

## How it works

This implementation is currently based on [Kevin Suffecool's poker hand evaluator](http://www.suffecool.net/poker/evaluator.html)  (aka Cactus Kev's Poker Hand Evaluator).
And [Paul Senzee's perfect hash implementation](http://www.paulsenzee.com/2006/06/some-perfect-hash.html).
And a nice [Java implementation of perfect hashing](https://github.com/baleful/Poker-Hand-Evaluator).
This perfect hashing implementation seems to be 2 to 3 times faster than binary searching for 100-1000 samples.
