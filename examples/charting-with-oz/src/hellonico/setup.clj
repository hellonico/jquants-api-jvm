(ns hellonico.setup
  (:require [oz.core :as oz]))

(defn livepreview [& args]
  (oz/start-server!)
  (oz/live-reload! "src/hellonico/charting.clj"))