(ns hellonico.jquants-api
  (:require [cheshire.core :as json])
  ; (:require [clojure.tools.logging :as log])
  (:require [org.httpkit.client :as http])
  (:require [next.jdbc :as jdbc] [next.jdbc.result-set :as rs][clojure.java.io :as io][honey.sql :as sql] [honey.sql.helpers :as h]))

(def config-folder (str (System/getProperty "user.home") "/.config/digima" ))
(def login-file (str config-folder "/login.edn" ))
(def refresh-token-file (str config-folder "/refresh_token.edn"))
(def id-token-file (str config-folder "/id_token.edn"))
(def cache-db (str config-folder "/jquants.db"))
(def cache-tmp (str config-folder "/cache.edn"))
(def ONE_DAY_IN_MS (* 24 3600 1000))
(def SEVEN_DAYS_IN_MS (* 7 24 3600 1000))

(def api-base "https://api.jpx-jquants.com/v1/")

(defn login[username password]
  (spit login-file {:mailaddress   username   :password  password}))

(defn refresh-refresh-token [ & args ]
  (let [body (json/generate-string (read-string (slurp login-file)))
        resp (http/post (str api-base "token/auth_user") {:body body})
        edn (json/parse-string (@resp :body) true)
        ]
    (println edn)
    edn))

(defn refresh-refresh-token-file[ & args]
  (spit refresh-token-file (refresh-refresh-token)))

(defn refresh-id-token [& args]
  (let [refreshToken  ((read-string (slurp refresh-token-file)) :refreshToken)
        url (str api-base "token/auth_refresh?refreshtoken=" refreshToken)
        resp (http/post url)
        ; _ (log/info ">" url)
        body (:body @resp)
        edn (json/parse-string body true)
        ]
    (println edn)
    edn))

(defn refresh-id-token-file [ & args]
    (spit id-token-file (refresh-id-token)))

(defn- get-id-token []
  ((read-string (slurp id-token-file)) :idToken))

(defn- authorization-headers []
  {:headers {"Authorization" (str "Bearer " (get-id-token))}})

(defn check-expired [file validity fn]
  (let [ time-difference (- (System/currentTimeMillis) (.lastModified (io/as-file file))) ]
    (if (> time-difference validity)
      (fn)
      (println "Up to date:" file " ( " time-difference " )"))))

(defn check-tokens []
  (check-expired id-token-file ONE_DAY_IN_MS refresh-id-token-file)
  (check-expired refresh-token-file SEVEN_DAYS_IN_MS refresh-refresh-token-file))

(defn get-json [endpoint]
  ;; (println "ENDPOINT:" endpoint) 
  (check-tokens)
  (let [resp (http/get endpoint (authorization-headers)) body (:body @resp) ]
    (println body)
    (json/parse-string body true)))

(defn listed-sections [& args]
  (get-json (str api-base "listed/sections")))

(defn listed-info [args]
  (get-json (str api-base "listed/info?code=" (args :code))))

(defn daily [args]
  (get-json (str api-base 
                 "prices/daily_quotes?" 
                 "code=" (args :code) 
                 (if (args :to) (str "&to=" (args :to) )"")
                 (if (args :from) (str "&from=" (args :from)) "")
                 "&date=" (args :date) )))

(defn statements [args]
  (get-json (str api-base "fins/statements?code=" (args :code) 
                 (if (args :date) (str "&date=" (args :date)) ""))))

;
;
;

(def db-spec {:dbtype "sqlite" :dbname cache-db})

(defn- cache-create-table []
  (jdbc/execute!
   db-spec
   [  "drop table if exists companies"])
  (jdbc/execute!
   db-spec
   [
    "create table if not exists companies (UpdateDate date, Code integer, CompanyNameFull text, CompanyName text, CompanyNameEnglish text, MarketCode Text, SectorCode Integer)"]))

(defn- cache-build [values]
  (cache-create-table)
  (jdbc/execute!
   db-spec
   (-> (h/insert-into :companies)
       (h/values (map #(dissoc % :origin) (filter #(not (nil? (% :UpdateDate))) values)))
       (sql/format {:pretty true}))))

(defn build-db-from-file [& args]
  (cache-build (read-string (slurp cache-tmp))))

(defn cache-to-json [& args]
  (println (json/generate-string (read-string (slurp cache-tmp)) {:pretty true})))

(defn build-code-database [& args]
  (let [raw (daily {:date 20220914}) 
        codes (map :Code (:daily_quotes raw)) 
        values (pmap #(assoc (first ((listed-info {:code %}) :info)) :origin %) codes)
        ]
    (spit cache-tmp (into [] values))
    (cache-build values)))

(def rebuild-info-cache build-code-database)

(defn check-cache [& args]
  (if (not (.exists (io/file cache-db)))
    (rebuild-info-cache)
    (println "Cache file exists:" cache-db)))

(defn fuzzy-search [args]
  (check-cache)
  (let [query 
        (str "select * from companies where "
             (if (args :MarketCode) (str "MarketCode like '" (args :MarketCode) "%'"))
             (if (args :SectorCode) (str "MarketCode like '" (args :SectorCode) "%'"))
             (if (args :CompanyName) (str "CompanyNameEnglish like '" (args :CompanyName) "%'"))
             (if (args :CompanyNameEnglish) (str "CompanyNameEnglish like '" (args :CompanyNameEnglish) "%'"))
             (if (args :Code) (str "CompanyNameEnglish like '" (args :Code) "%'"))
             ";")
        res (jdbc/execute! db-spec [query] {:builder-fn rs/as-unqualified-lower-maps})
        ]
    (println res)
    res
    ))

(defn daily-fuzzy[args]
  (let[ company (first (fuzzy-search args))
       search (merge args company)]
  (daily search)))