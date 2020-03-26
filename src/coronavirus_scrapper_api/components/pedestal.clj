(ns coronavirus-scrapper-api.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.interceptor.helpers :refer [before]]
            [io.pedestal.http.route :as route]
            [io.pedestal.http :as bootstrap]))

;;add the components to the routes
(defn- add-system [service]
  (before (fn [context] (assoc-in context [:request :components] service))))

(defn system-interceptors
  [service-map service]
  (update-in service-map
             [::bootstrap/interceptors] #(vec (->> % (cons (add-system service))))))

(defn prod-init [service-map]
  (bootstrap/default-interceptors service-map))

(defn base-prod-conf [routes port env]
  {:env                        env
   ::bootstrap/routes          #(route/expand-routes (deref routes))
   ::bootstrap/type            ::jetty
   ::bootstrap/allowed-origins (constantly true)
   ::bootstrap/port            port})

(defn service [config routes service]
  (let [env (:env config)
        port (:port config)
        service-conf (base-prod-conf routes port env)]
    (system-interceptors service-conf service)))

(defrecord Pedestal [config routes]
  component/Lifecycle
  (start [this]
    (assoc this :service (service (:config config) (:routes routes) this)))
  (stop [this]
    (assoc this :service nil)))

(defn new-service [] (map->Pedestal {}))