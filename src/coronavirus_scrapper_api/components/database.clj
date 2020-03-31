(ns coronavirus-scrapper-api.components.database
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as hikari]))

(def datasource-prod {
                      :auto-commit        true
                      :read-only          false
                      :connection-timeout 30000
                      :validation-timeout 5000
                      :idle-timeout       600000
                      :max-lifetime       1800000
                      :minimum-idle       10
                      :maximum-pool-size  10
                      :pool-name          "corona"
                      :adapter            "postgresql"
                      :username           (System/getenv "DATABASE_USR")
                      :password           (System/getenv "DATABASE_PWD")
                      :database-name      "postgresql"
                      :server-name        "localhost"
                      :port-number        5432
                      :register-mbeans    false})

(def datasource-dev {
                     :auto-commit        true
                     :read-only          false
                     :connection-timeout 30000
                     :validation-timeout 5000
                     :idle-timeout       600000
                     :max-lifetime       1800000
                     :minimum-idle       10
                     :maximum-pool-size  10
                     :pool-name          "corona"
                     :adapter            "postgresql"
                     :username           "covid"
                     :password           "covidpwd"
                     :database-name      "altrunox"
                     :server-name        "localhost"
                     :port-number        5432
                     :register-mbeans    false})

(defn datasource
  [ds]
  (hikari/make-datasource ds))

(defrecord DatabaseComponent [config]
  component/Lifecycle
  (start [this]
    (if (= :prod (:env config))
      (assoc this :database {:datasource (datasource datasource-prod)})
      (assoc this :database {:datasource (datasource datasource-dev)})))
  (stop [this]
    (assoc this :database nil))
  Object
  (toString [_] "<Database>"))

(defn new-database [] (map->DatabaseComponent {}))