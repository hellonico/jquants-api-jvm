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

(deftest daily-test-multiple
  (testing "daily code:86970 :date 20220118 (x2)"
    (is (=
         (into [] (repeat 2 (load-expected "daily_86970_20220118.json")))
         (daily [{:code 86970 :date 20220118}{:code 86970 :date 20220118}])))))

(deftest daily-fuzzy-test
  (testing "daily code:86970 :date 20220118"
    (is (= (load-expected "daily_86970_20220118.json")
           (daily-fuzzy {:CompanyNameEnglish "Japan Exchange" :date 20220118})))))

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
    (is (=
         (dissoc (first (:info (load-expected "listed_info_86970.json"))) :UpdateDate)
         (dissoc (first (:info (listed-info {:code 86970}))) :UpdateDate))))
  (testing "listed info code does not exist"
    (is (= (load-expected "listed_info_empty.json")
           (listed-info {:code 10000})))))

(deftest listed-sections-test
  (testing "listed sections"
    (is (= (load-expected "listed_sections.json")
           (listed-sections {})))))

(deftest refresh-tokens-test
  (testing "refresh id token"
    (let [res (refresh-id-token)]
      (is (contains? res :idToken))))

  (testing "refresh refresh token"
    (let [res (refresh-refresh-token)]
      (is (contains? res :refreshToken)))))