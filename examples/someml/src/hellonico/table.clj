(ns hellonico.table
  (:require [tablecloth.api :as tc]))


(require '[tablecloth.api :as tc])

(-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
    (tc/dataset {:key-fn keyword})
    (tc/group-by (fn [row]
                   {:symbol (:symbol row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:date row))}))
    (tc/aggregate #(tech.v3.datatype.functional/mean (% :price)))
    (tc/order-by [:symbol :year])
    (tc/head 10))

(require '[tech.v3.dataset :as ds])


(def a1 (ds/->dataset [{:a 1 :price 2} {:a 2 :price 3}]))
(tc/aggregate a1 #(tech.v3.datatype.functional/sum-fast (% :price)))




; Filter
; https://techascent.github.io/tech.ml.dataset/walkthrough.html
(def ames-ds (ds/->dataset "https://github.com/techascent/tech.ml.dataset/raw/master/test/data/ames-train.csv.gz"))
(tc/head ames-ds 10)
(ds/select ames-ds ["KitchenQual" "SalePrice"] [1 3 5 7 9])

(-> ames-ds (tc/dataset {:key-fn keyword}))

; stocks
(def stocks (ds/->dataset "https://github.com/techascent/tech.ml.dataset/raw/master/test/data/stocks.csv"))
(require '[tech.v3.datatype.functional :as dfn])

(def updated-ames
  (assoc ames-ds
    "TotalBath"
    (dfn/+ (ames-ds "BsmtFullBath")
           (dfn/* 0.5 (ames-ds "BsmtHalfBath"))
           (ames-ds "FullBath")
           (dfn/* 0.5 (ames-ds "HalfBath")))))
(ds/select-columns updated-ames ["Id" "MSSubClass"])

;(defonce stocks (tc/dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv" {:key-fn keyword}))

(def stocks (tc/dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv" {:key-fn keyword}))
;(tc/select stocks nil nil)
(tc/head stocks 6)

(tc/select-rows stocks #(= (:symbol %) "MSFT"))

(-> stocks
    (tc/group-by (fn [row]
                   {:symbol (:symbol row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:date row))}))
    (tc/aggregate #(tech.v3.datatype.functional/mean (% :price)))
    (tc/order-by [:symbol :year]))

(-> stocks
    (tc/group-by (juxt :symbol #(tech.v3.datatype.datetime/long-temporal-field :years (% :date))))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :price))}))
