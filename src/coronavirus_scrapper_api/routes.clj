(ns coronavirus-scrapper-api.routes
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [coronavirus-scrapper-api.postgresql :as database]
            [cheshire.core :as cs]
            [ring.util.response :as ring-resp]
            [coronavirus-scrapper-api.utils :as utils]))

(defn home-page
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (str "Hello, please check https://github.com/giovanialtelino/coronavirus-scrapper-api to check some docs of this API \n The last updated happened in: " (database/get-last-update-date (:database database))))
    "text/plain"))

(defn get-latest
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest (:database database))))
    "application/json"))

(defn get-latest-country
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest-by-country (:database database))))
    "application/json"))

(defn get-latest-country-timeline
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest-by-country-with-timeline (:database database))))
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
              ["/latest" :get (conj common-interceptors `get-latest) :route-name :get-latest]
              ["/latest-country" :get (conj common-interceptors `get-latest-country) :route-name :get-latest-by-country]
              ["/latest-country-timelines" :get (conj common-interceptors `get-latest-country-timeline) :route-name :get-latest-by-country-timeline]
              ["/locations" :get (conj common-interceptors `get-locations) :route-name :get-location]
              ["/locations/:id" :get (conj common-interceptors `get-location-id) :route-name :get-location-by-id]
              ["/postdata/:date" :post (conj common-interceptors `post-data) :route-name :post-location-json]
              ["/deletedata/:date" :delete (conj common-interceptors `delete-data) :route-name :delete-data-date]})