(ns coronavirus-scrapper-api.utils
  (:require [schema.core :as s]))

(def corona-cases
  {(s/optional-key :fips)           s/Int
   (s/optional-key :admin2)         s/Str
   (s/optional-key :province_state) s/Str
   :country_region                  s/Str
   (s/optional-key :last_update)    s/Any
   (s/optional-key :lat)            s/Num
   (s/optional-key :long)           s/Num
   :confirmed                       s/Num
   :deaths                          s/Num
   :recovered                       s/Num
   :active                          s/Num
   })

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