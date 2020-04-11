# coronavirus-scrapper-api

# CHANGING PROJECT TO GET DATA FROM THE [JHU](https://github.com/CSSEGISandData/COVID-19), ignore the readme for a while or use the older branch from the corona scrapper


This API get data from the [coronadatascraper](https://github.com/covidatlas/coronadatascraper) project.  
Process it, and save in a internal database for posterior use, which can be required as JSON.  
The main idea was to use it as an API to be queried by people doing studies with the Covid-19 data.  
It's also possible to require timelines for a country for example. Check the endpoints below.

Also keep in mind that the data is posted by users, not updated automatically, so it's pretty much a "community" database.
You may want to clone the project and use it internally. Fell free to clone this repo to do what you want, and also to suggest new endpoints.  
I will try to keep a public API open while we are still dealing with the covid-19 issue.

## REST Endpoints

"/" **GET**  
Get the last update date (Year - Month - Day) and also the github link.

"/latest" **GET**  
Sum the data of every country in the last updated date and return as below 
```json
{
"date":	"2020-10-22T03:00:00Z",
"tested":	2319,
"deaths":	12080,
"recovered":	85783,
"confirmed":	283289
}
```

"/latest-country" **GET**
Get the sum of the latest update by every country available.  

"/latest-country-timelines" **GET**
Get the sum of the latest update by every country available and also get the timeline from the start.

"/locations" **GET**  
Return the data of the last update of every country.
Can also request a single country using country_code as a query parameter, you should use a [iso3](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3) abbreviation, for example instead of "US" you should use "USA".
Timelines can also be requested as a query parameter, just add timelines=true. You can also request a single country with a timeline.

"/locations" **GET**  
```json
{
"aggregated":	"county",
"date":	"2020-10-22T03:00:00Z",
"iso3":	"USA",
"name":	"UNITED STATES",
"deaths":	null,
"county":	null,
"state": "MD" ,
"confirmed":	85,
"recovered":	null,
"population":	6045680,
"url":	"https://opendata.arcgis.…89a701354d7fa6830b_0.csv",
"index_id":	989,
"location":	null,
"tested":	null
}
```

"/locations?timelines=true" **GET**  
```json
{"aggregated":	"county",
"date":	"2020-10-22T03:00:00Z",
"iso3":	"USA",
"name":	"UNITED STATES",
"deaths":	null,
"county":	null,
"state": "MD" ,
"confirmed":	85,
"recovered":	null,
"population":	6045680,
"url":	"https://opendata.arcgis.…89a701354d7fa6830b_0.csv",
"index_id":	989,
"location":	null,
"tested":	null,
"timelines": [{"recovered":	null,
               "confirmed":	85,
               "deaths":	null,
               "tested":	null,
               "date":	"2020-10-22T03:00:00Z"},
              {}]
}	
```

"/locations/:id" **GET**  
You can get a specific location using the index_id in the path_parameter, every request will also return the timeline.

"/postdata/:date" **POST**  
The data to be saved in the internal database, should be posted as json, using something like Postman or Curl, since the coronadatascraper project don't save the date of their scrap in the json file, you should also specify as a path parameter the date of the json you are uploading, the date format that should be used is (YYYY-MM-DD).
The file format should be the same used in the coronascrapper, and can be found in the [Utils](src/coronavirus_scrapper_api/utils.clj) namespace.
As response, you will get the json values which were added.

An example of a curl post:  
```
curl -X POST -H "Content-Type: application/json" -d @./data.json  path/postdata/2020-10-22
```
              
"/deletedata/:date" **DELETE**        
If for some reason the added data is wrong, you just need to pass a specific date to delete it, in the same format as the post request (YYYY-MM-DD).

----------------------------
## Building your own API  
First, you must have a PostgreSQL database. Find the [database.sql](resources/database.sql) file in this project, inside the resources folder.  
You should run those queries to create the necessary tables.

You can keep the api running with lein run or lein run-dev, Leiningen is needed for this project.  
As it is, the port which will serve the endpoint in production is 8888, if you wish to change it go to the [Components](src/coronavirus_scrapper_api/components.clj) namespace.
To compile it just run **Lein Uberjar** and them run it as a standard java file, with java -jar, for example.  
In production Java will look for environment variables for the database, DATABASE_USR and DATABASE_PWD, and also uses the default database name of "postgres".
If you wish to change something here go to the [Database](src/coronavirus_scrapper_api/components/database.clj) namespace, like the path or port.

--------------------------
### MIT License