(ns hellonico.jquants-api
  (:require [cheshire.core :as json])
  (:require [org.httpkit.client :as http]))


(def config-folder (str (System/getProperty "user.home") "/.config/digima/" ))
(def login-file (str config-folder "/login.edn" ))
(def refresh-token-file (str config-folder "/refresh_token.edn"))
(def id-token-file (str config-folder "/id_token.edn"))

(def api-base "https://api.jpx-jquants.com/v1/")

(defn refresh-refresh-token []
  (let [body (json/generate-string (read-string (slurp login-file)))
        resp (http/post "https://api.jpx-jquants.com/v1/token/auth_user" {:body body})]
    (println (json/parse-string (@resp :body) true))))

(defn refresh-id-token []
  (let [refreshToken  ((read-string (slurp refresh-token-file)) :refreshToken)
        url (str "https://api.jpx-jquants.com/v1/token/auth_refresh?refreshtoken=" refreshToken)
        resp (http/post url)]
    (json/parse-string (:body @resp) true)))

(defn get-id-token []
  ((read-string (slurp id-token-file)) :idToken))

(defn authorization-headers []
  {:headers {"Authorization" (str "Bearer " (get-id-token))}})

(defn get-json [endpoint]
  ;; (println "ENDPOINT:" endpoint) 
  (let [resp (http/get endpoint (authorization-headers)) body (:body @resp) ]
    (println body)
    (json/parse-string body true)))

(defn listed-sections [args]
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