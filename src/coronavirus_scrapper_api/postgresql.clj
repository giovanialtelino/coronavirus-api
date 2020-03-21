(ns coronavirus-scrapper-api.postgresql
  (:require
    [com.stuartsierra.component :as compoentn]
    [clojure.java.jdbc :as jdbc]
    [java-time :as jt]
    [clojure.string :as string]))

(defn- pool-query
  ([conn query]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn query)]
                              result)))
  ([conn query args]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn [query args])]
                              result))))

(defn get-last-update-date [database]
  )

(defn get-latest [database]
  )

(defn get-location [database country_code timelines])

(defn get-location-by-id [database id])

(defn post-data [database date json])

(defn delete-data [database date])

create TABLE country
(id INTEGER PRIMARY KEY,
    name TEXT,
    abrev VARCHAR(10));

create TABLE coronavirus
(uuid INTEGER PRIMARY KEY,
      index_id INTEGER,
      country INTEGER REFERENCES country,
      province VARCHAR,
      location POINT,
      recovered INTEGER,
      confirmed INTEGER,
      deaths INTEGER,
      date DATE,
      url TEXT,
      population INTEGER
      );


