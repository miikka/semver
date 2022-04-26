(ns semver.core
  (:refer-clojure :exclude [satisfies?])
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [semver.parser :refer [parse-range]]))

(defn parse-version [version]
  (when version
    (->> (str/split version #"\.")
         (mapv #(Integer/valueOf %)))))

(deftest parse-version-test
  (is (= nil (parse-version nil)))
  (is (= [1 0 0] (parse-version "1.0.0"))))

(def operator-kw->fn {:< <, :> >, :<= <=, :>= >=, := =})

(def primitive-operators (set (keys operator-kw->fn)))

(defn pad [coll length value]
  (if (>= (count coll) length)
    coll
    (into [] (take length) (concat coll (repeat value)))))

(deftest pad-test
  (is (= [1 2 3] (pad [1 2 3] 1 0)))
  (is (= [1 2 3] (pad [1 2 3] 3 0)))
  (is (= [1 2 3 0 0 0] (pad [1 2 3] 6 0))))

(defmulti expand-range first)

(defmethod expand-range :default [range]
  [range])

(defmethod expand-range (keyword "^") [[_ version]]
  (let [[_ upper] (reduce (fn [[bumped? upper] component]
                            (if (or bumped? (zero? component))
                              [bumped? (conj upper 0)]
                              [true (conj upper (inc component))]))
                          [false []]
                          version)]
    [[:>= version], [:< upper]]))

(defmethod expand-range (keyword "~") [[_ version]]
  (let [upper (if (= 1 (count version))
                (map inc version)
                (into [(first version) (inc (second version))]
                      (map (constantly 0))
                      (drop 2 version)))]
    [[:>= (pad version 3 0)], [:< (pad upper 3 0)]]))

(defmethod expand-range :- [[_ lower upper]]
  [[:>= lower], [:<= upper]])

(defmethod expand-range :x [[_ version]]
  (when-let [lower (not-empty (into [] (take-while (complement #{:x}) version)))]
    (let [upper (update lower (dec (count lower)) inc)]
      [[:>= (pad lower 3 0)] [:< (pad upper 3 0)]])))

(deftest expand-range-test
  (is (= [[:= [1 2 3]]] (expand-range [:= [1 2 3]])))
  (is (= [[:>= [1 2 3]], [:< [2 0 0]]] (expand-range [(keyword "^") [1 2 3]])))
  (is (= [[:>= [0 2 3]], [:< [0 3 0]]] (expand-range [(keyword "^") [0 2 3]])))
  (is (= [[:>= [0 0 3]], [:< [0 0 4]]] (expand-range [(keyword "^") [0 0 3]])))
  (is (= [[:>= [1 2 3]], [:< [1 3 0]]] (expand-range [(keyword "~") [1 2 3]])))
  (is (= [[:>= [1 2 0]], [:< [1 3 0]]] (expand-range [(keyword "~") [1 2]])))
  (is (= [[:>= [1 0 0]], [:< [2 0 0]]] (expand-range [(keyword "~") [1]])))
  (is (= [[:>= [0 2 3]], [:< [0 3 0]]] (expand-range [(keyword "~") [0 2 3]])))
  (is (= [[:>= [0 2 0]], [:< [0 3 0]]] (expand-range [(keyword "~") [0 2]])))
  (is (= [[:>= [0 0 0]], [:< [1 0 0]]] (expand-range [(keyword "~") [0]])))
  (is (= [[:>= [1 2 3]], [:<= [4 5 6]]] (expand-range [:- [1 2 3] [4 5 6]])))
  (is (= nil (expand-range [:x [:x]])))
  (is (= [[:>= [1 0 0]], [:< [2 0 0]]] (expand-range [:x [1 :x]])))
  (is (= [[:>= [1 2 0]], [:< [1 3 0]]] (expand-range [:x [1 2 :x]]))))

(defn satisfies-range?
  [version [operator range-version]]
  (assert (contains? primitive-operators operator) (str "operator " (pr-str operator) " is not primitive"))
  ((operator-kw->fn operator) (compare version range-version) 0))

(defn satisfies?
  "Returns true if version satisfies range."
  [version range]
  (let [parsed-version (parse-version version)
        expanded-ranges (map expand-range (parse-range range))]
    (some #(every? (partial satisfies-range? parsed-version) %) expanded-ranges)))

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

(deftest satisfies?-tilde-test
  (is (satisfies? "1.2.3" "~1.2.3"))
  (is (satisfies? "1.2.4" "~1.2.3"))
  (is (not (satisfies? "1.2.0" "~1.2.3")))
  (is (not (satisfies? "1.3.0" "~1.2.3")))
  (is (not (satisfies? "2.0.0" "~1.2.3")))

  (is (satisfies? "1.2.0" "~1.2"))
  (is (satisfies? "1.2.1" "~1.2"))
  (is (not (satisfies? "1.1.0" "~1.2")))
  (is (not (satisfies? "1.3.0" "~1.2")))
  (is (not (satisfies? "2.0.0" "~1.2")))

  (is (satisfies? "1.0.0" "~1"))
  (is (satisfies? "1.0.3" "~1"))
  (is (satisfies? "1.2.0" "~1"))
  (is (not (satisfies? "0.1.0" "~1")))
  (is (not (satisfies? "2.0.0" "~1"))))

(deftest satisfies?-or-test
  (is (satisfies? "1.0.0" "1.0.0 || 2.0.0"))
  (is (satisfies? "2.0.0" "1.0.0 || 2.0.0"))
  (is (not (satisfies? "1.0.1" "1.0.0 || 2.0.0"))))

(deftest satisfies?-hyphen-test
  (is (satisfies? "1.0.0" "1.0.0 - 2.0.0"))
  (is (satisfies? "1.1.0" "1.0.0 - 2.0.0"))
  (is (satisfies? "2.0.0" "1.0.0 - 2.0.0"))
  (is (not (satisfies? "0.1.0" "1.0.0 - 2.0.0")))
  (is (not (satisfies? "2.1.0" "1.0.0 - 2.0.0"))))

(deftest satisfies?-x-test
  (is (satisfies? "1.0.0" "*"))
  (is (satisfies? "1.0.0" "1.x"))
  (is (satisfies? "1.1.0" "1.x"))
  (is (satisfies? "1.0.1" "1.x"))
  (is (not (satisfies? "0.1.0" "1.x")))
  (is (not (satisfies? "2.0.0" "1.x")))
  (is (satisfies? "1.0.0" "1.0.x"))
  (is (satisfies? "1.0.1" "1.0.x"))
  (is (not (satisfies? "1.1.0" "1.0.x")))
  (is (not (satisfies? "0.1.0" "1.0.x")))
  (is (not (satisfies? "2.0.0" "1.0.x"))))