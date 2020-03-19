(ns coronavirus-scrapper-api.components.schema
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [clojure.java.jdbc.spec :as jdbc]))

(def corona-cases
  {(s/optional-key :county)    String
   (s/optional-key :state)     String
   (s/optional-key :aggregate) String
   :recovered                  Integer
   :cases                      Integer
   :deaths                     Integer
   :url                        String
   :country                    String
   :rating                     Double
   :active                     Integer
   :featuredId                 Integer
   :population                 Integer
   :coordinates                [Float, Float]
   })

(def parse-corona-cases-json
  (coerce/coercer corona-cases coerce/json-coercion-matcher))

(defrecord Schema []
  component/Lifecycle
  (start [this]
    (assoc this :schema parse-corona-cases-json))
  (stop [this]
    (assoc this :schema nil)))

(defn new-schema [] (map->Schema {}))


