(ns coronavirus-scrapper-api.postgresql
  (:require
    [clojure.java.jdbc :as jdbc]
    [clj-postgresql.core :as pg]
    [java-time :as jt]
    [coronavirus-scrapper-api.utils :as utils]
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
  (into [] (drop 1 (pool-query database "SELECT last_update, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
   FROM coronavirus
   WHERE country_region = ?
   GROUP BY last_update
   ORDER BY last_update DESC" country-code))))

(defn- get-timeline-by-state [database state-code]
  (into [] (drop 1 (pool-query database "SELECT last_update, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
   FROM coronavirus
   WHERE province_state = ?
   GROUP BY last_update
   ORDER BY last_update DESC" state-code))))

(defn get-last-update-date-github [database]
  (:max (nth (pool-query database ["SELECT MAX (file_date) FROM coronavirus"]) 0)))

(defn- update-last-update [database]
  (let [last-date (get-last-update-date-github database)
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

(defn post-to-database [database date json]
  (let [point-vector (try-to-get-point [(:lat json) (:long json)])
        parsed-date (utils/github-date-parser (name date))]
    (jdbc/insert! database :coronavirus {:admin2         (:admin2 json)
                                         :fips           (:fips json)
                                         :province_state (:province_state json)
                                         :country_region (:country_region json)
                                         :last_update    (:last_update json)
                                         :location       point-vector
                                         :file_date      parsed-date
                                         :recovered      (:recovered json)
                                         :confirmed      (:confirmed json)
                                         :deaths         (:deaths json)
                                         :active         (:active json)
                                         :tested         (:tested json)})))

(defn post-data [database json-map]
  (let [json-count (count json-map)]
    (loop [i 0]
      (if (> json-count i)
        (let [date (key (first (nth json-map i)))
              json (date (nth json-map i))]
          (loop [f 0]
            (if (> (count json) f)
              (do
                (post-to-database database date (nth json f))
                (recur (inc f)))))))
      (recur (inc i)))))

(defn- check-if-update-needed [database]
  (let [local-last-update (get-last-update database)
        plus-3 (jt/plus local-last-update (jt/hours 3))
        now (jt/local-date-time)]
    (if (jt/after? now plus-3)
      (do
        (reset! last-update now)
        (post-data database (slurper/slurp-date-vector local-last-update)))
      (do
        "No need to update"))))

(defn get-latest [database]
  (async/go (check-if-update-needed database))
  (first (pool-query database "SELECT file_date, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
    FROM coronavirus
    WHERE file_date = ?
    GROUP BY file_date" (get-last-update-date-github database))))

(defn get-all-by-date [database date]
  (async/go (check-if-update-needed database))
  (first (pool-query database "SELECT file_date, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
    FROM coronavirus
    WHERE file_date = ?
    GROUP BY file_date" (utils/date-checker-parser date))))

(defn get-locations [database country_region province_state timelines date]
  (async/go (check-if-update-needed database))
  (let [search-date (if (nil? date)
                      (get-last-update-date-github database)
                      (utils/date-checker-parser date))
        country-query "SELECT country_region, file_date, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
                        FROM coronavirus
                        WHERE file_date = ? AND country_region = ?
                        GROUP BY country_region, file_date
                        ORDER BY country_region ASC"
        state-query "SELECT province_state, file_date, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
                        FROM coronavirus
                        WHERE file_date = ? AND province_state = ?
                        GROUP BY province_state, file_date
                        ORDER BY province_state ASC"
        country-state-query "SELECT province_state, country_region, file_date, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested, SUM(active) as active
                        FROM coronavirus
                        WHERE file_date = ? AND province_state = ? AND country_region = ?
                        GROUP BY province_state, file_date, country_region
                        ORDER BY country_region ASC, province_state ASC"
        query-result (into [] (cond
                                (and (not (nil? country_region)) (not (nil? province_state))) (pool-query database [country-state-query search-date province_state country_region])
                                (not (nil? country_region)) (pool-query database [country-query search-date country_region])
                                (not (nil? province_state)) (pool-query database [state-query search-date province_state])))]
    (if (nil? timelines)
      query-result
      query-result)))

(defn get-latest-by-country [database]
  (async/go (check-if-update-needed database))
  (pool-query database "SELECT file_date, country_region, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested,
                        SUM(active) as active
                        FROM coronavirus
                        WHERE file_date =?
                        GROUP BY country_region, file_date
                        ORDER BY country_region DESC" (get-last-update-date-github database)))

(defn get-latest-by-country-date [database date]
  (async/go (check-if-update-needed database))
  (pool-query database "SELECT file_date, country_region, SUM(confirmed) AS confirmed, SUM(deaths) AS deaths, SUM(recovered) AS recovered, SUM(tested) AS tested,
                        SUM(active) as active
                        FROM coronavirus
                        WHERE file_date =?
                        GROUP BY country_region, file_date
                        ORDER BY country_region DESC" (utils/date-checker-parser date)))

(defn get-latest-by-country-with-timeline [database]
  (async/go (check-if-update-needed database))
  (let [without-timeline (into [] (get-latest-by-country database))]
    (loop [i 0
           map-with-timeline without-timeline]
      (if (< i (count without-timeline))
        (let [index-country (get-timeline-by-country database (:country_region (nth without-timeline i)))
              merged-map (merge (nth without-timeline i) {:timelines index-country})]
          (recur (inc i) (assoc map-with-timeline i merged-map)))
        map-with-timeline))))

(defn- all-dates [database]
  (into [] (map :file_date (pool-query database "SELECT DISTINCT(file_date) FROM coronavirus ORDER BY file_date ASC"))))

(defn- all-countries [database]
  (into [] (map :country_region (pool-query database "SELECT DISTINCT(country_region) FROM coronavirus ORDER BY country_region ASC"))))

(defn- all-states [database]
  (into [] (map :province_state (pool-query database "SELECT DISTINCT(province_state) FROM coronavirus ORDER BY province_state ASC"))))

(defn get-search-variables [database]
  {:dates          (all-dates database)
   :country_region (all-countries database)
   :province_state (all-states database)})

(defn- delete-since-last-update [database date]
  (if (jt/after? (jt/local-date-time date) (update-last-update database))
    "Requested date is greater than the last update in the database. Nothing to do."
    (do
      (jdbc/delete! database :coronavirus ["file_date >= ?" date])
      (reset! last-update (jt/local-date-time date))
      (async/go (check-if-update-needed database))
      (str "Data deleted since " date ", updating the database again."))))

(defn retroactive-slurper [database pwd date]
  (let [pwd-valid (utils/check-if-pwd-valid pwd)
        parsed-date (try
                      (utils/date-checker-parser date)
                      (catch Exception e
                        "Date format was wrong. Use: yyyy-MM-dd (full year - numeric month - day)"))]
    (if (true? pwd-valid)
      (delete-since-last-update database parsed-date)
      pwd-valid)))