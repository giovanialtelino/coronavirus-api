(ns coronavirus-scrapper-api.routes
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [coronavirus-scrapper-api.postgresql :as database]
            [cheshire.core :as cs]
            [ring.util.response :as ring-resp]
            [coronavirus-scrapper-api.utils :as utils]))

(defn home-page
  [request]
  (ring-resp/content-type
    (ring-resp/response
      "Hello, please check https://github.com/giovanialtelino/coronavirus-scrapper-api to check some docs of this API")
    "text/plain"))

(defn get-latest
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest (:database database))))
    "application/json"))

;country_code or timelines true or false
(defn get-locations
  [{{:keys [country_code timelines]} :query-params
    {:keys [database]}               :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-locations (:database database) country_code timelines)))
    "application/json"))

(defn get-location-id
  [{{:keys [id]}       :path-params
    {:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-location-by-id (:database database) id)))
    "application/json"))

; (database/post-data (:database database) date json)
;;Not sure why,  but if I try to extract the keys as below I get an error on the :json-params
;{{:keys [date]}     :path-params {:keys [database]} :components {:keys [json]}     :json-params}
(defn post-data
  [request]
  (let [edn-data (:json-params request)
        date (:date (:path-params request))
        components (:components request)
        database (:database (:database components))]
    (ring-resp/content-type
      (ring-resp/response
        (cs/generate-string (database/post-data database date (utils/schema-parser (vec edn-data)))))
      "application/json")))

;{:datasource #object[com.zaxxer.hikari.HikariDataSource 0x21f341f0 "HikariDataSource (corona)"]}


(defn delete-data
  [{{:keys [date]}     :path-params
    {:keys [database]} :components}]
  (try
    (database/delete-data (:database database) date)
    (ring-resp/response "Data deleted")
    (catch Exception e
      (ring-resp/response (str "Error while deleting: " e)))))

(def common-interceptors [(body-params/body-params) bootstrap/html-body])

(def routes #{["/" :get (conj common-interceptors `home-page) :route-name :index]
              ["/v2/latest" :get (conj common-interceptors `get-latest) :route-name :get-latest]
              ["/v2/locations" :get (conj common-interceptors `get-locations) :route-name :get-location]
              ["/v2/locations/:id" :get (conj common-interceptors `get-location-id) :route-name :get-location-by-id]
              ["/postdata/:date" :post (conj common-interceptors `post-data) :route-name :post-location-json]
              ["/deletedata/:date" :delete (conj common-interceptors `delete-data) :route-name :delete-data-date]})