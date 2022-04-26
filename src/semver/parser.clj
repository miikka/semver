(ns semver.parser
  (:require [clojure.test :refer [deftest is]]
            [instaparse.core :as insta]))

;; Based on https://github.com/npm/node-semver/blob/238acc4d8bc1e9dfd0f64c0356aea9deb0434049/README.md#range-grammar
(def range-parser
  (insta/parser
   "<range-set>  ::= range ( <logical-or> range ) *
logical-or   ::= ( ' ' ) * '||' ( ' ' ) *
<range>      ::= hyphen | simple ( ' ' simple ) * | ''
hyphen       ::= partial <' - '> partial
simple       ::= primitive | partial | tilde | caret
primitive    ::= ( '<' | '>' | '>=' | '<=' | '=' ) partial
partial      ::= xr ( <'.'> xr ( <'.'> xr qualifier ? )? )?
<xr>         ::= 'x' | 'X' | '*' | nr
<nr>         ::= '0' | #'[1-9]' ( #'[0-9]' ) *
tilde        ::= <'~'> partial
caret        ::= <'^'> partial
qualifier    ::= ( '-' pre )? ( '+' build )?
pre          ::= parts
build        ::= parts
parts        ::= part ( '.' part ) *
part         ::= nr | #'[-0-9A-Za-z]+'"))

(defn parse-xr [s]
  (if (contains? #{"x" "X" "*"} s)
    :x
    (parse-long s)))

(defn process-partial [[_ & xs]]
  (mapv parse-xr xs))

(defn- third [coll] (nth coll 2))

(defn process [[op xs :as input]]
  (if (= op :hyphen)
    (into [:-] (map process-partial) (rest input))
    (case (first xs)
      :partial (let [p (process-partial xs)]
                 (if (some #{:x} p)
                   [:x p]
                   [:= p]))
      :tilde [(keyword "~") (process-partial (second xs))]
      :caret [(keyword "^") (process-partial (second xs))]
      :primitive [(keyword (second xs)) (process-partial (third xs))])))

(defn parse-range [range-str]
  (mapv process (range-parser range-str)))

(deftest parse-range-test
  (is (= [[:= [1 0 0]]] (parse-range "1.0.0")))
  (is (= [[:= [1 0 0]]] (parse-range "=1.0.0")))
  (is (= [[:>= [1 0 0]]] (parse-range ">=1.0.0")))
  (is (= [[(keyword "~") [1 0 0]]] (parse-range "~1.0.0")))
  (is (= [[:= [1 0 0]], [:= [2 0 0]]] (parse-range "1.0.0 || 2.0.0")))
  (is (= [[:- [1 0 0] [2 0 0]]] (parse-range "1.0.0 - 2.0.0")))
  (is (= [[:x [1 0 :x]]] (parse-range "1.0.x")))
  (is (= [[:x [1 :x 0]]] (parse-range "1.*.0"))))