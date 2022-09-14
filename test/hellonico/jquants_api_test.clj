(ns hellonico.jquants-api-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [hellonico.jquants-api :refer :all]
            [cheshire.core :as json]))

(defn load-expected [data-file]
  (json/parse-string (slurp (io/resource data-file)) true))

(deftest daily-test
  (testing "daily code:86970 :date 20220118"
    (is (= 
        (load-expected "daily_86970_20220118.json")
         (daily {:code 86970 :date 20220118})))))

(deftest statements-test
  (testing "daily code:86970 :date 20220727"
    (is (= (load-expected "statements_expected.json")
      (statements {:code 86970 :date 20220727})))))