(ns hellonico.knnants
  (:require [scicloj.ml.core :as ml]
            [scicloj.ml.dataset :as ds]
            [hellonico.jquants-api :as api]
            [scicloj.ml.metamorph :as mm]))

(require '[scicloj.ml.core :as ml]
  '[scicloj.ml.dataset :as ds]
  '[hellonico.jquants-api :as api]
  '[scicloj.ml.metamorph :as mm])

(def companyNameEnglish "Sony")
(def quotes
  (:daily_quotes (api/daily-fuzzy {:CompanyNameEnglish companyNameEnglish :from 20201002 :to 20220930})))

(def df
  (ds/dataset {:x (map #(Integer/parseInt (% :Date)) quotes)
               :y (map :Close quotes)}))

(def pipe-fn
  (ml/pipeline
    (mm/set-inference-target :y)
    (mm/categorical->number [:y])
    (mm/model
      {:model-type :smile.classification/knn
       :k 20})))

(def trained-ctx
  (pipe-fn {:metamorph/data df
            :metamorph/mode :fit}))

(defn guess [date]
  (->
    trained-ctx
    (merge
      {:metamorph/data (ds/dataset {:x [date]  :y [nil]}) :metamorph/mode :transform})
      pipe-fn
    :metamorph/data (ds/column-values->categorical :y)))

(guess 20220720)
; 4032


(defn one-quote [date]
  (:Close (first (:daily_quotes (api/daily-fuzzy {:CompanyNameEnglish companyNameEnglish :date date})))))

(one-quote 20220719)
; 3944


