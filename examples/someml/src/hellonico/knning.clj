(ns hellonico.knning
  (:require [scicloj.ml.core :as ml]
         [scicloj.ml.dataset :as ds]
         [scicloj.ml.metamorph :as mm]))

(def df
  (ds/dataset {:x1 [7 7 3 1]
               :x2 [0 3 4 4]
               :y [ 7 10 7 5]}))

(def pipe-fn
  (ml/pipeline
    (mm/set-inference-target :y)
    (mm/categorical->number [:y])
    (mm/model
      {:model-type :smile.classification/knn
       :k 1})))

(def trained-ctx
  (pipe-fn {:metamorph/data df
            :metamorph/mode :fit}))


(->
  trained-ctx
  (merge
    {:metamorph/data (ds/dataset
                       {:x1 [3]
                        :x2 [2]
                        :y [nil]})
     :metamorph/mode :transform})
  pipe-fn
  :metamorph/data
  (ds/column-values->categorical :y)
  )
