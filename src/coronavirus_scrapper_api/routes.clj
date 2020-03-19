(ns coronavirus-scrapper-api.routes
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [coronavirus-scrapper-api.components.database :as database]
            [ring.util.response :as ring-resp]))

(defn home-page
  [request]
  (ring-resp/response "Hello, please check https://github.com/giovanialtelino/coronavirus-scrapper-api to check some docs of this API"))

(defn get-latest
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/resource-response
      (database/get-latest database))
    "application/json"))

;country_code or timelines true or false
(defn get-location
  [{{:keys [country_code timelines]} :query-params
    {:keys [database]}               :components}]
  (ring-resp/content-type
    (ring-resp/resource-response
      (database/get-location database country_code timelines))
    "application/json"))

(defn get-location-id
  [{{:keys [id]}       :path-params
    {:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/resource-response
      (database/get-full-location-by-id database id))
    "application/json"))

(defn post-data
  [{{:keys [date]}     :path-params
    {:keys [database]} :components
    {:keys [json]}     :json-params}]
  (ring-resp/resource-response "not yet implemented"))

(defn delete-data
  [{{:keys [date]}     :path-params
    {:keys [database]} :components}]
  (try
    (database/delete-data database date)
    (ring-resp "Data deleted")
    (catch Exception e
      (ring-resp (str "Error while deleting: " e)))))

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/" :get (conj common-interceptors `home-page) :route-name :index]
              ["/v2/latest" :get (conj common-interceptors `get-latest) :route-name :get-latest]
              ["/v2/locations" :get (conj common-interceptors `get-location) :route-name :get-location]
              ["/v2/locations/:id" :get (conj common-interceptors `get-location-id) :route-name :get-location-by-id]
              ["/postdata/:date" :post (conj common-interceptors `post-data) :route-name :post-location-json]
              ["/deletedata/:date" :delete (conj common-interceptors `delete-data) :route-name :delete-data-date]
              })

