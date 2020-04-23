(ns coronavirus-scrapper-api.postgresql
  (:require
    [clojure.java.jdbc :as jdbc]
    [clj-postgresql.core :as pg]
    [java-time :as jt]
    [coronavirus-scrapper-api.slurper :as slurper]
    [clojure.core.async :as async]))

(def last-update (atom nil))

(defn- pool-query
  ([conn query]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn query)]
                              result)))
  ([conn query args]
   (jdbc/with-db-connection [pool-conn conn]
                            (let [result (jdbc/query pool-conn [query args])]
                              result))))

(defn- get-timeline-by-country [database country-code]
  (into [] (pool-query database "SELECT CO.date, SUM (CO.tested) AS tested, SUM (CO.deaths) AS deaths, SUM (CO.recovered) AS recovered, SUM (CO.confirmed) AS confirmed
   FROM coronavirus CO
   WHERE country = ?
   GROUP BY CO.date
   ORDER BY date DESC" country-code)))

(defn get-timeline-by-id [database id]
  (into [] (pool-query database "SELECT recovered, confirmed, deaths, tested, date FROM coronavirus WHERE index_id = ? ORDER BY date DESC" id)))

(defn get-last-update-date [database]
  (:max (nth (pool-query database ["SELECT MAX (date) FROM coronavirus"]) 0)))

(defn- update-last-update [database]
  (let [last-date (get-last-update-date database)
        date-to-atom (if (nil? last-date)
                       (jt/local-date-time 2020 01 22)
                       (jt/local-date-time last-date))]
    (reset! last-update date-to-atom)
    @last-update))

(defn- get-last-update [database]
  (let [last-update @last-update]
    (if (nil? last-update)
      (update-last-update database)
      last-update)))

(defn- try-to-get-point [point-vector]
  (try
    (pg/point (Double/parseDouble (first point-vector)) (Double/parseDouble (second point-vector)))
    (catch Exception e
      nil)))

(defn post-data [database date json]
  (loop [f (dec (count json))]
    (if (<= 0 f)
      (do
        (loop [i (dec (count (nth json f)))]
          (if (<= 0 i)
            (do
              (let [current-i (nth (nth json f) i)
                    point-vector (try-to-get-point [(:lat current-i) (:long current-i)])
                    return (jdbc/insert! database :coronavirus {:admin2         (:admin2 current-i)
                                                                :fips           (:fips current-i)
                                                                :province_state (:province_state current-i)
                                                                :country_region (:country_region current-i)
                                                                :last_update    (:last_update current-i)
                                                                :date           (jt/to-sql-date date)
                                                                :location       point-vector
                                                                :recovered      (:recovered current-i)
                                                                :confirmed      (:confirmed current-i)
                                                                :deaths         (:deaths current-i)
                                                                :active         (:active current-i)
                                                                :tested         (:tested current-i)})]
                (recur (dec i))))))
        (recur (dec f))))))

(defn- check-if-update-needed [database]
  (let [local-last-update (get-last-update database)
        plus-3 (jt/plus local-last-update (jt/hours 3))
        now (jt/local-date-time)]
    (if (jt/after? now plus-3)
      (do
        (reset! last-update (jt/local-date-time))
        (post-data database now (slurper/slurp-date-vector local-last-update)))
      (do
        "No need to update"))))
;coronavirus-scrapper-api.postgresql=> (jt/plus @last-update (jt/hours 8))

;;;helpers above
(defn get-latest [database]
  ;(async/go (check-if-update-needed database))
  (check-if-update-needed database)
  (let [last-update (get-last-update-date database)]
    (first (pool-query database "SELECT CO.date, SUM (CO.tested) AS tested, SUM (CO.deaths) AS deaths, SUM (CO.recovered) AS recovered, SUM (CO.confirmed) AS confirmed
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
  (async/go (check-if-update-needed database))
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
  (async/go (check-if-update-needed database))
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
  (async/go (check-if-update-needed database))
  (pool-query database " SELECT id FROM country WHERE iso3 = ? " country-abrev))


(defn get-latest-by-country [database]
  (async/go (check-if-update-needed database))
  (pool-query database "SELECT CO.date, SUM (CO.tested) AS tested, SUM (CO.deaths) AS deaths, SUM (CO.recovered) AS recovered,
                      SUM (CO.confirmed) AS confirmed, CT.iso3 AS iso3, CT.name AS name, CO.country AS country_index
                      FROM coronavirus CO
                      INNER JOIN country CT ON CO.country = CT.id
                      WHERE CO.date = ?
                      GROUP BY CO.date, CO.country, CT.name, CT.iso3
                      ORDER BY CT.iso3 ASC" (get-last-update-date database)))

(defn get-latest-by-country-with-timeline [database]
  (async/go (check-if-update-needed database))
  (let [without-timeline (into [] (get-latest-by-country database))]
    (loop [i 0
           map-with-timeline without-timeline]
      (if (< i (count without-timeline))
        (let [index-country (get-timeline-by-country database (:country_index (nth without-timeline i)))
              merged-map (merge (nth without-timeline i) {:timelines index-country})]
          (recur (inc i) (assoc map-with-timeline i merged-map)))
        map-with-timeline))))



(defn delete-data [database date]
  (jdbc/delete! database :coronavirus [" date = ? " (jt/to-sql-date date)]))

