(ns coronavirus-scrapper-api.components.servlet
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as bootstrap]))

(defrecord Servlet [service]
  component/Lifecycle
  (start [this]
    (assoc this :instance (-> service
                              :service
                              (assoc ::bootstrap/join? false)
                              bootstrap/create-server
                              bootstrap/start)))
  (stop [this]
    (bootstrap/stop (:instance this))
    (assoc this :instance nil)))

(defn new-servlet [] (map->Servlet {}))

