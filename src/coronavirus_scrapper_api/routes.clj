(ns coronavirus-scrapper-api.routes
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [coronavirus-scrapper-api.postgresql :as database]
            [cheshire.core :as cs]
            [ring.util.response :as ring-resp]
            [coronavirus-scrapper-api.utils :as utils]
            [coronavirus-scrapper-api.slurper :as slurper]))

(defn home-page
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (str "Hello, please check https://github.com/giovanialtelino/coronavirus-scrapper-api to check some docs of this API \n The last updated happened in: " (database/get-last-update-date (:database database))))
    "text/html"))

(defn get-latest
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest (:database database))))
    "application/json"))

(defn get-all-by-date
  [{{:keys [database]} :components
    {:keys [date]}     :path-params}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-all-by-date (:database database) date)))
    "application/json"))

(defn get-latest-country
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest-by-country (:database database))))
    "application/json"))

(defn get-latest-country-date
  [{{:keys [database]} :components
    {:keys [date]}     :path-params}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest-by-country-date (:database database) date)))
    "application/json"))

(defn get-latest-country-timeline
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-latest-by-country-with-timeline (:database database))))
    "application/json"))

;country_code or timelines true or false
(defn get-locations
  [{{:keys [country_region province_state date timelines]} :query-params
    {:keys [database]}                                     :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-locations (:database database) country_region province_state timelines date)))
    "application/json"))

(defn get-search-variables
  [{{:keys [database]} :components}]
  (ring-resp/content-type
    (ring-resp/response
      (cs/generate-string (database/get-search-variables (:database database))))
    "application/json"))

(def common-interceptors [(body-params/body-params) bootstrap/html-body])

(def routes #{["/" :get (conj common-interceptors `home-page) :route-name :index]
              ["/latest" :get (conj common-interceptors `get-latest) :route-name :get-latest]
              ["/all-date/:date" :get (conj common-interceptors `get-all-by-date) :route-name :get-all-by-date]
              ["/latest-country" :get (conj common-interceptors `get-latest-country) :route-name :get-latest-by-country]
              ["/latest-country/:date" :get (conj common-interceptors `get-latest-country-date) :route-name :get-latest-by-country-by-date]
              ["/latest-country-timelines" :get (conj common-interceptors `get-latest-country-timeline) :route-name :get-latest-by-country-timeline]
              ["/search-variables" :get (conj common-interceptors `get-search-variables) :route-name :get-search-variables]
              ["/locations" :get (conj common-interceptors `get-locations) :route-name :get-location]})