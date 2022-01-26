(ns semver.core
  (:refer-clojure :exclude [satisfies?])
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(defn parse-version [version]
  (when version
    (->> (str/split version #"\.")
         (mapv #(Integer/valueOf %)))))

(deftest parse-version-test
  (is (= nil (parse-version nil)))
  (is (= [1 0 0] (parse-version "1.0.0"))))

(defn parse-range [range]
  (let [[_ operator version] (re-matches #"(<|<=|>|>=|=|\^)?([0-9.]+)" range)]
    [(if operator (keyword operator) :=) (parse-version version)]))

(deftest parse-range-test
  (is (= [:= [1 0 0]] (parse-range "1.0.0")))
  (is (= [:= [1 0 0]] (parse-range "=1.0.0")))
  (is (= [:>= [1 0 0]] (parse-range ">=1.0.0"))))

(def operator-kw->fn {:< <, :> >, :<= <=, :>= >=, := =})

(defn expand-range [[operator version :as range]]
  (cond
    (contains? operator-kw->fn operator) [range]
    (= (keyword "^") operator)
    (let [[_ upper] (reduce (fn [[bumped? upper] component]
                          (if (or bumped? (zero? component))
                            [bumped? (conj upper 0)]
                            [true (conj upper (inc component))]))
                        [false []]
                        version)]
      [[:>= version]
       [:< upper]])))

(deftest expand-range-test
  (is (= [[:= [1 2 3]]] (expand-range [:= [1 2 3]])))
  (is (= [[:>= [1 2 3]], [:< [2 0 0]]] (expand-range [(keyword "^") [1 2 3]])))
  (is (= [[:>= [0 2 3]], [:< [0 3 0]]] (expand-range [(keyword "^") [0 2 3]])))
  (is (= [[:>= [0 0 3]], [:< [0 0 4]]] (expand-range [(keyword "^") [0 0 3]]))))

(defn satisfies-range? [version [operator range-version]]
  ((operator-kw->fn operator) (compare version range-version) 0))

(defn satisfies?
  "Returns true if version satisfies range."
  [version range]
  (let [parsed-version (parse-version version)
        expanded-ranges (expand-range (parse-range range))]
    (every? (partial satisfies-range? parsed-version) expanded-ranges)))

(deftest satisfies?-test
  (is (satisfies? "1.0.0" "1.0.0"))
  (is (not (satisfies? "1.0.0" "2.0.0")))
  (is (satisfies? "1.0.0" "=1.0.0"))
  (is (not (satisfies? "1.0.0" "=2.0.0")))
  (is (satisfies? "1.0.0" ">=1.0.0"))
  (is (satisfies? "1.0.0" ">=0.1.0"))
  (is (not (satisfies? "1.0.0" ">=1.0.1")))
  (is (not (satisfies? "1.0.0" ">=1.1.0")))
  (is (not (satisfies? "1.0.0" ">=2.0.0")))
  (is (satisfies? "2.0.0" ">1.0.0"))
  (is (satisfies? "1.0.1" ">1.0.0"))
  (is (satisfies? "0.0.1" ">0.0.0"))
  (is (not (satisfies? "1.0.0" ">1.0.0")))
  (is (satisfies? "0.1.0" "<1.0.0"))
  (is (not (satisfies? "1.0.0" "<1.0.0")))
  (is (satisfies? "1.0.0" "<=1.0.0"))
  (is (not (satisfies? "1.0.0" "<=0.1.0"))))

(deftest satisfies?-caret-test
  (is (satisfies? "1.2.3" "^1.2.3"))
  (is (satisfies? "1.3.3" "^1.2.3"))
  (is (satisfies? "1.2.4" "^1.2.3"))
  (is (not (satisfies? "1.2.2" "^1.2.3")))
  (is (not (satisfies? "2.0.0" "^1.2.3")))
  (is (satisfies? "0.2.3" "^0.2.3"))
  (is (satisfies? "0.2.4" "^0.2.3"))
  (is (not (satisfies? "0.2.2" "^0.2.3")))
  (is (not (satisfies? "1.2.3" "^0.2.3")))
  (is (not (satisfies? "0.3.3" "^0.2.3")))
  (is (satisfies? "0.0.3" "^0.0.3"))
  (is (not (satisfies? "0.0.2" "^0.0.3")))
  (is (not (satisfies? "0.0.4" "^0.0.3")))
  (is (not (satisfies? "0.1.0" "^0.0.3")))
  (is (not (satisfies? "1.0.0" "^0.0.3"))))