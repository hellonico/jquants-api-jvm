(ns hellonico.jquants-api
  (:gen-class
   :name hellonico.jquantsapi
   :prefix "-"
   :init init
   :main true
   :methods [[daily [String String] java.util.Map]
             [daily [String String String] java.util.Map]
             [statements [String String] java.util.Map]
             [listedInfo [String] java.util.Map]
             [companySearch [java.util.Map] java.util.Map]
             [login [String String] void]
             [refreshRefreshToken [] void]
             [refreshIdToken [] void]
             [dailyFuzzy [java.util.Map] java.util.Map]])
  (:require
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [clojure.tools.logging :as log]
   [org.httpkit.client :as http]
   [cheshire.core :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [honey.sql :as sql]
   [honey.sql.helpers :as h]))

(def ^:no-doc config-folder (str (System/getProperty "user.home") "/.config/jquants" ))
(def ^:no-doc login-file (str config-folder "/login.edn" ))
(def ^:no-doc refresh-token-file (str config-folder "/refresh_token.edn"))
(def ^:no-doc id-token-file (str config-folder "/id_token.edn"))
(def ^:no-doc cache-db (str config-folder "/jquants.db"))
(def ^:no-doc cache-tmp (str config-folder "/cache.edn"))
(def ^:no-doc ONE_DAY_IN_MS (* 24 3600 1000))
(def ^:no-doc SEVEN_DAYS_IN_MS (* 7 24 3600 1000))

(def ^:no-doc state (atom {:usekeywords true}))

(def ^:no-doc api-base "https://api.jpx-jquants.com/v1/")

(defn login [& args]
  (println args)
  (clojure.java.io/make-parents login-file)
  (spit login-file {:mailaddress  (:mailaddress (first args))  :password  (:password (first args))}))

(defn refresh-refresh-token [ & args ]
  (let [body (json/generate-string (read-string (slurp login-file)))
        url (str api-base "token/auth_user")
        resp (http/post url {:body body})
        edn (json/parse-string (@resp :body) true)]
    (log/log 'jquants.internal :debug nil (str "Refresh refresh token:" url))
    (log/log 'jquants.internal :debug nil (str "Refresh refresh token:\n" edn))
    (println edn)
    edn))

(defn refresh-refresh-token-file[ & args]
  (spit refresh-token-file (refresh-refresh-token)))

(defn refresh-id-token [& args]
  (let [refreshToken  ((read-string (slurp refresh-token-file)) :refreshToken)
        url (str api-base "token/auth_refresh?refreshtoken=" refreshToken)
        resp (http/post url)
        body (:body @resp)
        edn (json/parse-string body true)
        ]
    (log/log 'jquants.internal :debug nil (str "Refresh id token:" url))
    (log/log 'jquants.internal :debug nil (str "Refresh id token:\n" edn))
    (println edn)
    edn))

(defn refresh-id-token-file [ & args]
    (spit id-token-file (refresh-id-token)))

(defn get-id-token []
  ((read-string (slurp id-token-file)) :idToken))

(defn- ^:no-doc authorization-headers []
  {:headers {"Authorization" (str "Bearer " (get-id-token))}})

(defn- ^:no-doc check-expired [file validity fn]
  (if (not (.exists (clojure.java.io/as-file file)))
    (fn)
    (let [time-difference (- (System/currentTimeMillis) (.lastModified (io/as-file file)))]
      (if (> time-difference validity)
        (fn)
        (log/log 'jquants.internal :debug nil (str "Token up to date:" file " ( " time-difference " )"))))))

(defn- ^:no-doc check-tokens []
  (check-expired refresh-token-file SEVEN_DAYS_IN_MS refresh-refresh-token-file)
  (check-expired id-token-file ONE_DAY_IN_MS refresh-id-token-file))

(defn get-json 
  [endpoint]
   (log/log 'jquants-api.http :info nil (str endpoint "> [" @state "]"))
   (check-tokens)
   (let [resp (http/get endpoint (authorization-headers)) body (:body @resp) edn (json/parse-string body (:usekeywords @state))]
     ; (log/log 'jquants-api.http :info nil body)
     (log/log 'jquants-api.http :info nil edn)
     (println body)
     edn))

(defn listed-sections [& args]
  (get-json (str api-base "listed/sections")))

(defn listed-info [args]
  (get-json (str api-base "listed/info?code=" (args :code))))

(defn daily [args]
  (if (map? args)
    (let [res      (get-json (str api-base
                                  "prices/daily_quotes?"
                                  "code=" (args :code)
                                  (if (args :to) (str "&to=" (args :to)) "")
                                  (if (args :from) (str "&from=" (args :from)) "")
                                  "&date=" (args :date)))
          quotes (filter #(not (nil? (:Close %))) (:daily_quotes res))]
      (hash-map :daily_quotes quotes)
      )
    ;(filter #(not (nil? (:Close %)))

    (into [] (map daily args))))

(defn statements [args]
  (get-json (str api-base "fins/statements?code=" (args :code) 
                 (if (args :date) (str "&date=" (args :date)) ""))))

;
; fuzzy search section
;

(def ^:no-doc db-spec {:dbtype "sqlite" :dbname cache-db})

(defn- ^:no-doc cache-create-table []
  (jdbc/execute!
   db-spec
   [  "drop table if exists companies"])
  (jdbc/execute!
   db-spec
   [
    "create table if not exists companies (UpdateDate date, Code integer, CompanyNameFull text, CompanyName text, CompanyNameEnglish text, MarketCode Text, SectorCode Integer)"]))

(defn- ^:no-doc cache-build [values]
  (cache-create-table)
  (jdbc/execute!
   db-spec
   (-> (h/insert-into :companies)
       (h/values (map #(dissoc % :origin) (filter #(not (nil? (% :UpdateDate))) values)))
       (sql/format {:pretty true}))))

(defn ^:no-doc build-db-from-file [& args]
  (cache-build (read-string (slurp cache-tmp))))

(defn ^:no-doc cache-to-json [& args]
  (println (json/generate-string (read-string (slurp cache-tmp)) {:pretty true})))

(defn ^:no-doc build-code-database [& args]
  (let [raw (daily {:date 20220914}) 
        codes (map :Code (:daily_quotes raw)) 
        values (pmap #(assoc (first ((listed-info {:code %}) :info)) :origin %) codes)
        ]
    (spit cache-tmp (into [] values))
    (cache-build values)))

(def ^:no-doc rebuild-info-cache build-code-database)

(defn ^:no-doc check-cache [& args]
  (if (not (.exists (io/file cache-db)))
    (rebuild-info-cache)
    (log/log 'jquants.internal :debug nil (str "Cache file exists:" cache-db))))

(defn fuzzy-search [args]
  (check-cache)
  (let [query 
        (str "select * from companies where "
             (if (args :MarketCode) (str "MarketCode like '" (args :MarketCode) "%'") "") 
             (if (args :SectorCode) (str "MarketCode like '" (args :SectorCode) "%'") "")
             (if (args :CompanyName) (str "CompanyNameEnglish like '" (args :CompanyName) "%'") "")
             (if (args :CompanyNameEnglish) (str "CompanyNameEnglish like '" (args :CompanyNameEnglish) "%'") "")
             (if (args :Code) (str "Code like '" (args :Code) "%'") "")
             ";")
        res (jdbc/execute! db-spec [query] {:builder-fn rs/as-unqualified-lower-maps})
        ]
    (log/log 'jquants.internal :info nil query)
    (log/log 'jquants.internal :info nil res)
    res
    ))

(defn daily-fuzzy [args]
  ;(if (map? args)
  (println args)
  (if (instance? java.util.Map args)
    (let [company (first (fuzzy-search args))
          search (merge args company)]
      (daily search))
    (into [] (map daily-fuzzy args))))

; java

(defn ^:no-doc -init[]
  (reset! state {:usekeywords false})
  [[] (ref {})])

(defn -daily 
  ([_ code date]
   (daily {:code code :date date}))
  ([_ code from to]
   (daily {:code code :from from :to to})))

(defn -statements[_ code date]
  (statements {:code code :date date}))

(defn -listedInfo[_ code]
  (listed-info {:code code}))

(defn- ^:no-doc stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))]
    (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

;; (defn string-keys [m]
;;   (into (empty m) (for [[k v] m] [(.toUpperCase (name k)) v])))

;; (defn keyword-keys [m]
;;   (into (empty m) (for [[k v] m] [(keyword (.toLowerCase k)) v])))

(defn -companySearch[_ args]
  (stringify-keys (fuzzy-search args)))

(defn -dailyFuzzy [_ args]
  ;; (println (type args))
  ;; (println (map? args))
  (stringify-keys (daily-fuzzy (walk/keywordize-keys (into (hash-map) args)))))

(defn -login [_ email password]
  (login {:mailaddress email :password password}))

(defn -refreshRefreshToken[_]
  (refresh-refresh-token-file))

(defn -refreshIdToken[_]
  (refresh-id-token-file))

(defn -main [& args]
  (println "Does nothing yet"))