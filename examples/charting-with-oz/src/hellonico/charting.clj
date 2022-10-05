(ns hellonico.charting)

(require '[hellonico.jquants-api :as api])
(require '[oz.core :as oz])

(def raw (api/daily {:code 2413 :from 20220301 :to 20220328}))

(def data (raw :daily_quotes))

(defn average [coll]
  (/ (reduce + coll)
     (count coll)))

(defn ma [period coll]
  (lazy-cat (repeat (dec period) nil)
            (map average (partition period 1  coll))))
(def ma-5 (partial ma 5))

(def _data (map #(assoc %1 :ma %2) data (ma-5 (map :High data))))

; (spit "test.edn" data)
; (def data (map #(assoc % :ma (ma 5 (% :High))) (raw :daily_quotes)))

(def data-plot
  {:data {:values _data}
   :encoding {:x {:field "Date" :type "ordinal"}
              :y {:field "ma" :type "quantitative"}
              :color {:field "Code" :type "nominal"}}
   :mark "line"}
  )

(def viz
  [:div
   [:h1 "5 days Moving average"]
   [:p "Entity code 24130"]
   [:p "from 20220301 to 20220328"]
   [:vega-lite data-plot]
   ;[:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
   [:p (str (java.util.Date.))]
   ])

(oz/view! viz)
