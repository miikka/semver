(ns semver.core
  (:refer-clojure :exclude [satisfies?])
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(defn parse-version [version]
  (->> (str/split version #"\.")
       (map #(Integer/valueOf %))))

(deftest parse-version-test
  (is (= [1 0 0] (parse-version "1.0.0"))))

(defn parse-range [range]
  (let [[_ operator version] (re-matches #"(<|<=|>|>=|=)?([0-9.]+)" range)]
    [(if operator (keyword operator) :=) (parse-version version)]))

(deftest parse-range-test
  (is (= [:= [1 0 0]] (parse-range "1.0.0")))
  (is (= [:>= [1 0 0]] (parse-range ">=1.0.0"))))

(defmulti satisfies-range? (fn [_version range] (first range)))

(defmethod satisfies-range? := [version [_ range-version]]
  (= version range-version))

;; TODO(miikka) Replace the code below with... just using compare on the version vectors

(defmethod satisfies-range? :>= [version [_ range-version]]
  (->> (map compare version range-version)
       (drop-while zero?)
       (first)
       ((fnil nat-int? 0))))

(defmethod satisfies-range? :> [version [_ range-version]]
  (->> (map compare version range-version)
       (drop-while zero?)
       (first)
       ((fnil pos? 0))))

(defn satisfies?
  "Returns true if version satisfies range."
  [version range]
  (satisfies-range? (parse-version version) (parse-range range)))

(deftest satisfies?-test
  (is (satisfies? "1.0.0" "1.0.0"))
  (is (not (satisfies? "1.0.0" "2.0.0")))
  (is (satisfies? "1.0.0" ">=1.0.0"))
  (is (satisfies? "1.0.0" ">=0.1.0"))
  (is (not (satisfies? "1.0.0" ">=1.0.1")))
  (is (not (satisfies? "1.0.0" ">=1.1.0")))
  (is (not (satisfies? "1.0.0" ">=2.0.0")))
  (is (satisfies? "2.0.0" ">1.0.0"))
  (is (satisfies? "1.0.1" ">1.0.0"))
  (is (satisfies? "0.0.1" ">0.0.0"))
  (is (not (satisfies? "1.0.0" ">1.0.0"))))