(ns coronavirus-scrapper-api.utils
  (:require [schema.core :as s]))

(def corona-cases
  {(s/optional-key :county)      s/Str
   (s/optional-key :state)       s/Str
   (s/optional-key :aggregate)   s/Str
   (s/optional-key :tested)      s/Int
   (s/optional-key :recovered)   s/Int
   :cases                        s/Int
   (s/optional-key :deaths)      s/Int
   :url                          s/Str
   :country                      s/Str
   :rating                       s/Num
   :active                       s/Int
   (s/optional-key :featureId)   s/Int
   (s/optional-key :population)  s/Int
   (s/optional-key :coordinates) [s/Num]})

(defn- remove-i [v i]
  (into (subvec v 0 i) (subvec v (inc i))))

(defn- check-if-invalid [edn-map index]
  (try
    (s/validate corona-cases edn-map)
    nil
    (catch Exception e
      index
      )))

(defn- find-invalid-positions [edn-map]
  (let [map-count (count edn-map)]
    (sort (loop [invalid-vector []
                 i 0]
            (if (< i map-count)
              (recur (conj invalid-vector (check-if-invalid (nth edn-map i) i)) (inc i))
              invalid-vector)))))

(defn- cleaned-map [edn-map invalid-indexes]
  (loop [cleaned edn-map
         count (dec (count invalid-indexes))]
    (if (>= count 0)
      (recur (remove-i cleaned (nth invalid-indexes count)) (dec count))
      cleaned)))

(defn schema-parser [edn-map]
  (let [invalid (remove nil? (find-invalid-positions edn-map))]
    (if (empty? invalid)
      edn-map
      (cleaned-map edn-map invalid))))

;{:county "Illinois County", :cases 0, :tested 0, :state "IL", :country "USA", :url "http://www.dph.illinois.gov/sites/default/files/COVID19/COVID19CountyResults.json", :rating 0.43902439024390244, :active 0}