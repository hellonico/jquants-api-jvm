(ns hellonico.jquants-api
  (:require [cheshire.core :as json])
  (:require [clojure.tools.logging :as log])
  (:require [org.httpkit.client :as http]))


(def config-folder (str (System/getProperty "user.home") "/.config/digima/" ))
(def login-file (str config-folder "/login.edn" ))
(def refresh-token-file (str config-folder "/refresh_token.edn"))
(def id-token-file (str config-folder "/id_token.edn"))

(def api-base "https://api.jpx-jquants.com/v1/")

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

(defn get-json [endpoint]
  ;; (println "ENDPOINT:" endpoint) 
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