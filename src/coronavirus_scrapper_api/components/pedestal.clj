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

(defn dev-init [service-map]
  (-> service-map
      bootstrap/default-interceptors
      bootstrap/dev-interceptors))

(defn base-prod-conf [routes port env]
  {:env               env
   ::bootstrap/routes #(route/expand-routes (deref routes))
   ::bootstrap/type   ::jetty
   ::bootstrap/port   port})

(defn service [config routes service]
  (let [env (:env config)
        port (:port config)
        service-conf (base-prod-conf (:routes routes) port env)]
    (-> (if (= :prod env)
          (prod-init service-conf)
          (dev-init service-conf))
        (system-interceptors service))))

(defrecord Pedestal [config routes]
  component/Lifecycle
  (start [this]
    (assoc this :service (service config routes this)))
  (stop [this]
    (assoc this :service nil)))

(defn new-service [] (map->Pedestal {}))