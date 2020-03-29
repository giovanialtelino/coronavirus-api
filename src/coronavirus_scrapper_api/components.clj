(ns coronavirus-scrapper-api.components
  (:require [com.stuartsierra.component :as component]
            [coronavirus-scrapper-api.components.config :as config]
            [coronavirus-scrapper-api.components.database :as database]
            [coronavirus-scrapper-api.components.pedestal :as pedestal]
            [coronavirus-scrapper-api.components.routes :as routes]
            [coronavirus-scrapper-api.components.servlet :as servlet]
            [coronavirus-scrapper-api.routes]))

(def prod-config-map {:env  :prod
                      :port 8080})

(def dev-config-map {:env  :dev
                     :port 8080})

(def deps [:config :routes :database])

(defn prod-components [config-map]
  (component/system-map
    :config (config/new-config config-map)
    :database (component/using (database/new-database) [:config])
    :routes (routes/new-routes #'coronavirus-scrapper-api.routes/routes)
    :service (component/using (pedestal/new-service) deps)
    :servlet (component/using (servlet/new-servlet) [:service])
    ))

(defn dev-components [config-map]
  (component/system-map
    :config (config/new-config config-map)
    :database (component/using (database/new-database) [:config])
    :routes (routes/new-routes #'coronavirus-scrapper-api.routes/routes)
    :service (component/using (pedestal/new-service) deps)
    :servlet (component/using (servlet/new-servlet) [:service])
    ))

(defn create-and-start-system! [config-map]
  (if (= (:env config-map) :prod)
    (prod-components config-map)
    (if (= (:env config-map) :dev)
      (dev-components config-map)
      (prn "WRONG :env VARIABLES, check the config-maps"))))

(defn create-dev-system! []
  (->
    dev-config-map
    create-and-start-system!
    component/start))

(defn create-prod-system! []
  (->
    prod-config-map
    create-and-start-system!
    component/start))