(defproject poker-hand-evaluator "0.2.0"
  :description "Poker Hand Evaluator"
  :url "https://github.com/travisstaloch/clojure-poker-hand-evaluator"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.4"]]

  :profiles {:debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             ;; activated by default
             :dev {
                   :dependencies [;;[clj-stacktrace "0.2.4"]
                                  [org.clojure/test.check "0.9.0"]
                                  ;;[com.taoensso/tufte "1.1.1"]
                                  ;;[criterium "0.4.4"]
                                  ;;[clj-stacktrace "0.2.8"]
                                  [environ "1.0.0"]
                                  ]
                   :plugins [[quickie "0.2.5"]]
                   :env {:performance-test false}
                   }
             ;; activated automatically during uberjar
             ;;:uberjar {:aot :all}
             ;; activated automatically in repl task
             ;;:repl {:plugins [[cider/cider-nrepl "0.7.1"]]}
             }
  :clean-targets ^{:protect false} []
  :main poker-hand-evaluator.core
  ;;:javac-options ["-XX:-DontCompileHugeMethods"]
  ;;:jvm-opts ["-XX:-DontCompileHugeMethods"]
  ;;:debug true
  :resource-paths ["src/poker_hand_evaluator"]
  )
