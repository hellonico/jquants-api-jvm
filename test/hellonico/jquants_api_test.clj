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
    (is (= (load-expected "statements_86970_20220118.json")
      (statements {:code 86970 :date 20220727}))))
  (testing "code is invalid"
    (is (= (load-expected "statements_6digitscode.json")
           (statements {:code 869701 :date 20220727}))))
  (testing "code is does not exist"
    (is (= (load-expected "statements_empty.json")
           (statements {:code 10000 :date 20220727})))))

(deftest listed-info-test
  (testing "listed info code:86970"
    (is (= (load-expected "listed_info_86970.json")
           (listed-info {:code 86970})))))

(deftest listed-sections-test
  (testing "listed sections"
    (is (= (load-expected "listed_sections.json")
           (listed-sections {})))))