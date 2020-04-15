(ns coronavirus-scrapper-api.slurper
  (:require [java-time :as jt]
            [clojure.data.csv :as csv]
            [clojure.string :as clo-str]))

;Slurper? What a name
;(def starter-date (jt/local-date 2020 01 22))
;(defn get-last-update [database starter-date]
;(let [x (database/get-last-update-date database)]
;(if (nil? x)
;  starter-date
;  x) ) )

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


(defn get-date-vector-until-today [last-update]
  (let [today (jt/local-date-time)]
    (loop [current-date last-update
           missing-dates []]
      (if (jt/after? current-date today)
        missing-dates
        (recur (jt/plus current-date (jt/days 1)) (into missing-dates [current-date]))))))

(defn- find-correct-key [k-name]
  (let [k (clo-str/trim (str k-name))
        tested (case k
                 "Province/State" :province_state
                 "Country/Region" :country_region
                 "Last Update" :last_update
                 "Lat" :lat
                 "Long_" :long
                 "Confirmed" :confirmed
                 "Deaths" :deaths
                 "Recovered" :recovered
                 "Active" :active
                 "FIPS" :fips
                 "Admin2" :admin2
                 "Tested" :tested
                 :nil
                 )]
    (if (nil? tested)
      (prn (str "PARSED INTO NIL" k)))
    [tested]))

(defn- clean-keys [k]
  (loop [clean-k []
         i 0]
    (if (< i (count k))
      (recur (into clean-k (find-correct-key (nth k i))) (inc i))
      clean-k
      )))

(defn- add-keys [k v]
  (loop [new-map {}
         i 0]
    (if (< i (count k))
      (recur (into new-map {(nth k i) (nth v i)}) (inc i))
      new-map)))

(defn csv-to-clojure-map [c]
  (let [v (vec c)
        k (first v)
        cleaned-k (clean-keys k)
        without-keywords (subvec v 1)
        vector-count (count without-keywords)]
    (loop [csv-mapped []
           i 0]
      (if (< i vector-count)
        (recur (conj csv-mapped (add-keys cleaned-k (nth without-keywords i))) (inc i))
        csv-mapped))))

(defn slurp-date-vector [last-update]
  (let [dates-vector (get-date-vector-until-today last-update)
        every-csv-link (check-last-date (map github-url dates-vector))
        every-slurp (map slurp every-csv-link)
        parsed-to-csv (map csv/read-csv every-slurp)]
    (map csv-to-clojure-map parsed-to-csv)))