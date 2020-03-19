(ns coronavirus-scrapper-api.server
  (:gen-class)                                              ; for -main method in uberjar
  (:require [coronavirus-scrapper-api.components :as component]))

(defonce runnable-service (server/create-server service/service))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating your [DEV] server...")
  (component/create-dev-system!))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (component/create-prod-system!))
