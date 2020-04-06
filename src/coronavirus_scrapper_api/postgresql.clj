(ns coronavirus-scrapper-api.postgresql
  (:require
    [clojure.java.jdbc :as jdbc]
    [clj-postgresql.core :as pg]
    [java-time :as jt]))

(defn- pool-query
  ([conn query]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn query)]
                              result)))
  ([conn query args]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn [query args])]
                              result))))

(defn get-timeline-by-id [database id]
  (into [] (pool-query database "SELECT recovered, confirmed, deaths, tested, date FROM coronavirus WHERE index_id = ? ORDER BY date DESC" id)))

(defn get-last-update-date [database]
  (:max (nth (pool-query database ["SELECT MAX (date) FROM coronavirus"]) 0)))

;(pool-query database "SELECT CT.name, CT.iso3, CO.date, SUM(CO.tested) AS tested, SUM(CO.deaths) AS deaths, SUM(CO.recovered) AS recovered, SUM(CO.confirmed) AS confirmed
;    FROM coronavirus CO
;    INNER JOIN country CT ON CO.country = CT.id
;    WHERE date=?
;    GROUP BY CO.date, CO.country, CT.name, CT.iso3
;    ORDER BY CO.date" last-update)

(defn get-latest [database]
  (let [last-update (get-last-update-date database)]
    (first (pool-query database " SELECT CO.date, SUM (CO.tested) AS tested, SUM (CO.deaths) AS deaths, SUM (CO.recovered) AS recovered, SUM (CO.confirmed) AS confirmed
    FROM coronavirus CO
    WHERE date=?
     GROUP BY CO.date" last-update))))

(defn- loop-and-merge-maps [covid-map database]
  (loop [i 0
         map-with-timeline covid-map]
    (if (< i (count covid-map))
      (let [index-timeline (get-timeline-by-id database (:index_id (nth covid-map i)))
            merged-map (merge (nth covid-map i) {:timelines index-timeline})]
        (recur (inc i) (assoc map-with-timeline i merged-map)))
      map-with-timeline)))

(defn get-locations [database country_code timelines]
  (let [last-update (get-last-update-date database)
        all-query " SELECT CO.index_id, Ct.iso3, CT.name, CO.state, CO.county, CO.location, CO.recovered, CO.confirmed, CO.tested, CO.deaths, CO.url, CO.population, CO.aggregated, CO.date
FROM coronavirus CO
INNER JOIN country CT ON CO.country = CT.id
WHERE date=?
ORDER BY Ct.iso3 ASC "
        country-code-query " SELECT CO.index_id, CT.iso3, CT.name, CO.state, CO.county, CO.location, CO.recovered, CO.confirmed, CO.deaths, CO.tested, CO.url, CO.population, CO.aggregated, CO.date
FROM coronavirus CO
INNER JOIN country CT ON CO.country = CT.id
WHERE CT.iso3 = ? AND date = ?
ORDER BY date DESC "
        query (into [] (if (nil? country_code)
                         (pool-query database all-query last-update)
                         (pool-query database [country-code-query country_code last-update])))]
    (if (nil? timelines)
      query
      (loop-and-merge-maps query database))))

(defn get-location-by-id [database id]
  (let [id-integer (Integer/parseInt id)
        last-update (get-last-update-date database)
        without-timeline (first (pool-query database [" SELECT CO.index_id, Ct.iso3, CT.name, CO.state, CO.county, CO.location, CO.recovered, CO.confirmed, CO.deaths, CO.tested CO.url, CO.population, CO.aggregated, CO.date
FROM coronavirus CO
INNER JOIN country CT ON CO.country = CT.id
WHERE CO.date= ? AND CO.index_id= ?
ORDER BY Ct.iso3 ASC " last-update id-integer]))
        index-timeline (get-timeline-by-id database (:index_id without-timeline))]
    (merge without-timeline {:timelines index-timeline})))

(defn- get-country-id [database country-abrev]
  (pool-query database " SELECT id FROM country WHERE iso3 = ? " country-abrev))

(defn post-data [database date json]
  (loop [i (dec (count json))
         added []]
    (if (>= i 0)
      (do
        (let [current-i (nth json i)
              country-id (:id (first (get-country-id database (:country current-i))))
              return (jdbc/insert! database :coronavirus {:index_id   (:featureId current-i)
                                                          :country    country-id
                                                          :state      (:state current-i)
                                                          :county     (:county current-i)
                                                          :recovered  (:recovered current-i)
                                                          :confirmed  (:cases current-i)
                                                          :deaths     (:deaths current-i)
                                                          :url        (:url current-i)
                                                          :population (:population current-i)
                                                          :aggregated (:aggregate current-i)
                                                          :tested     (:tested current-i)
                                                          :rating     (:rating current-i)
                                                          :active     (:active current-i)
                                                          :date       (jt/to-sql-date date)})]
          (recur (dec i) (into added return))))
      added)))

(defn delete-data [database date]
  (jdbc/delete! database :coronavirus [" date = ? " (jt/to-sql-date date)]))

