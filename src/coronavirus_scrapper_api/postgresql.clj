(ns coronavirus-scrapper-api.postgresql
  )

(defn get-latest [database])

(defn get-location [database country_code timelines])

(defn get-location-by-id [database id])

(defn post-data [database date json])

(defn delete-data [database date])

