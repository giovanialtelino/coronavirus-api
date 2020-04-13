(ns coronavirus-scrapper-api.slurper
  (:require [java-time :as jt]
            [coronavirus-scrapper-api.postgresql :as database]
            [clojure.data.csv :as csv]))

;Slurper? What a name
(def starter-date (jt/local-date 2020 01 22))

;errr 1 hour to find butlast....
(defn check-last-date [date-vector]
  (try
    (slurp (last date-vector))
    date-vector
    (catch Exception e
      (butlast date-vector))))

(defn github-url [date]
  (str "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/" (jt/format "MM-dd-YYYY" date) ".csv"))

; (database/get-last-update-date database)
(defn get-last-update [database starter-date]
  (let [x nil]
    (if (nil? x)
      starter-date
      x)))

(defn get-date-vector-until-today [database]
  (let [today (jt/local-date)
        starter-date starter-date
        last-update (get-last-update database starter-date)]
    (loop [current-date last-update
           missing-dates []]
      (if (jt/after? current-date today)
        missing-dates
        (recur (jt/plus current-date (jt/days 1)) (into missing-dates [current-date]))))))

admin2 VARCHAR(50),
fips INTEGER,
index_id INTEGER,
country INTEGER REFERENCES country,
province_state VARCHAR,
Country_Region VARCHAR,
last_update DATE,
date DATE,
location POINT,
recovered INTEGER,
confirmed INTEGER,
deaths INTEGER,
active integer,
tested INTEGER

(defn- vector-to-map [k v]
  ;need to check first which position is which key, since new fields were added from the start

  ;loop every position and replace the values with the correct keys
  ;then loop the v and create maps with the keys


  )

(defn csv-to-clojure-map [c]
  (let [keywords (map keyword (first c))
        without-keywords (subvec c 1)
        vector-count (count without-keywords)]
    (loop [csv-mapped []
           i 0]
      (if (< i vector-count)
        (recur (into csv-mapped (vector-to-map keywords (nth without-keywords i))) (inc i))
        csv-mapped))))

(defn slurp-date-vector [database]
  (let [dates-vector (get-date-vector-until-today database)
        every-csv-link (check-last-date (map github-url dates-vector))
        every-slurp (map slurp every-csv-link)
        parsed-to-csv (map csv/read-csv every-slurp)
        mapped (map csv-to-clojure-map parsed-to-csv)]
    (prn (first mapped))))