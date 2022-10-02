(ns hellonico.jquanting)

(require '[hellonico.jquants-api :as api])
(require '[tablecloth.api :as tc])

(def quotes (:daily_quotes (api/daily-fuzzy {:CompanyNameEnglish "M3"})))

; step 0
(-> quotes
    (tc/dataset)
    (tc/row-count))

; step1
(-> quotes
     (tc/dataset)
     (tc/head 5))


; step2
(-> quotes
    (tc/dataset {:key-fn keyword})
    (tc/drop-columns [:Low :AdjustmentVolume :TurnoverValue :AdjustmentHigh :AdjustmentOpen :AdjustmentLow :AdjustmentFactor :AdjustmentClose ])
    (tc/head 5))

(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/select-columns [:Open :High :Date])
    (tc/head 5))

; step3
(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
               {:code (:Code row)
                :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))})))

; step4
(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
                   {:code (:Code row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))}))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :Open))}))

; step5
(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
                   {:code (:Code row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))}))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :Open))}))

; step 6
(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
                   {:code (:Code row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))}))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :Open))})
    (tc/select-columns [:year :avg])
    (tc/order-by [:avg] :desc))

; step 7

(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
                   {:code (:Code row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))}))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :Open))})
    (tc/select-columns [:year :avg])
    (tc/order-by [:avg] :desc))

(-> quotes
    (tc/dataset {:key-fn keyword :parser-fn {:Date [:local-date "yyyyMMdd"]}})
    (tc/group-by (fn [row]
                   {:code (:Code row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:Date row))}))
    (tc/aggregate {:avg #(tech.v3.datatype.functional/mean (% :Open))})
    (tc/select-columns [:year :avg])
    (tc/order-by [:avg] :desc)
    ;(tc/write-csv! "hello.csv")
    (tc/write! "test.csv.gz")
    )


