# coronavirus-scrapper-api

This API get data from the [JHU](https://github.com/CSSEGISandData/COVID-19) project, using their [daily update](https://github.com/CSSEGISandData/COVID-19/tree/master/csse_covid_19_data/csse_covid_19_daily_reports).  
This project process their data, and save in a PostgreSQL database for posterior use, which can be required as JSON.  
The main idea was to use it as an API to be queried by people doing studies with the Covid-19 data, and wanted to use the JHU data.
It's also possible to require timelines for a country or region, for example. Check the endpoints below.
The 'tested' field is null in most cases, since they aren't included in the daily reports.
  
You may want to clone the project and use it internally, since I'm hosting it in the cheapest Digital Ocean Droplet with other projects.
Fell free to clone this repo to do what you want, also to suggest new endpoints and corrections.
  
Keep in mind that sometimes the JHU team update some old data, and the software doesn't automatically check for updated files, it only looks for new files, if you are using my endpoint and see that they updated some old data, DM me at [Twitter](https://twitter.com/GioAltelino), or if you don't have Twitter open an issue here.  
I will try to keep a public API open while we are still dealing with the COVID-19.  
[https://covid-api.giovanialtelino.com/](https://covid-api.giovanialtelino.com/).

## REST Endpoints

**Difference between file_date and last_update**
The file_date is the date of the file present in the JHU daily reports. The last_update is the last_time the data for that country were updated.

"/" **GET**  [TEST](https://covid-api.giovanialtelino.com/)  
Get the last update date (Year - Month - Day) and also the github link.

"/latest" **GET**  [TEST](https://covid-api.giovanialtelino.com/latest)  
Sum the data of every country in the last updated date and return as below 
```json
{
 "file_date": "2020-04-26T03:00:00Z",
    "confirmed": 32686225,
    "deaths": 2271984,
    "recovered": 9523063,
    "tested": null,
    "active": 20891178
}
```

"/all-date/:date" **GET** [TEST](https://covid-api.giovanialtelino.com/all-data/2020-02-20)  
Same as '/latest', but get a specific date instead of the last one.
Must use the date format as yyyy-MM-dd, full year, numeric month and day, in this order.

"/latest-country" **GET** [TEST](https://covid-api.giovanialtelino.com/latest-country)  
Get the latest update of every country available in the database.
```json
[
      {"file_date": "2020-04-26T03:00:00Z",
        "last_update": "2020-04-27T03:00:00Z",
        "country_region": "Zimbabwe",
        "confirmed": 372,
        "deaths": 48,
        "recovered": 24,
        "tested": null,
        "active": 300
    },
    {
        "file_date": "2020-04-26T03:00:00Z",
        "last_update": "2020-04-27T03:00:00Z",
        "country_region": "Zambia",
        "confirmed": 1056,
        "deaths": 36,
        "recovered": 504,
        "tested": null,
        "active": 516
    }
]
```              

"/all-country/:date" **GET** [TEST](https://covid-api.giovanialtelino.com/all-country/2020-02-20)  
Same as '/latest-country', but get a specific date instead of the last one.
Must use the date format as yyyy-MM-dd, full year, numeric month and day, in this order.

"/all-country-timelines/:date" **GET** [TEST](https://covid-api.giovanialtelino.com/latest-country-timelines)  
Return the data as '/latest-country', but also request the timeline of the last_update for every country, since the last date.
```json
[{
        "file_date": "2020-04-26T03:00:00Z",
        "last_update": "2020-04-27T03:00:00Z",
        "country_region": "Zimbabwe",
        "confirmed": 248,
        "deaths": 32,
        "recovered": 16,
        "tested": null,
        "active": 200,
        "timelines": [
            {
                "last_update": "2020-04-25T03:00:00Z",
                "confirmed": 29,
                "deaths": 4,
                "recovered": 2,
                "tested": null,
                "active": 23
            },
            {
                "last_update": "2020-04-24T03:00:00Z",
                "confirmed": 28,
                "deaths": 4,
                "recovered": 2,
                "tested": null,
                "active": 22
            }]
}
]
```

"/all-country-timelines/:date" **GET** [TEST](https://covid-api.giovanialtelino.com/search-variables)  
Returns the search variables that can be used in the 'location' endpoint below.
```json
{
    "date": [],
    "country_region": [],  
    "province_state": []
}
```

"/locations" **GET** [TEST](https://covid-api.giovanialtelino.com/locations?country_region=Brazil&timelines=true&date=2020-04-22)  
Here we use query parameters instead of path parameters. Check the example above to check how to write query parameters, if needed.  
The options are country_region, province_state, timelines, date.  
The ones obligatory are country_region or province_state, you can also request both, in case you are worried that more than one country_region have the same province_state.  
You can request a specific date, using again the format yyyy-MM-dd, if left blank will return the last data available.  
You can request the timeline of the specific country_region or province_state, it will return only the values below the specified date, or every disposable value if no date was used.
To request the timeline just add 'timelines=true' to the query.

"/locations" **GET** [TEST](https://covid-api.giovanialtelino.com/retroactive/:pwd/:date)  
Will delete every data from the database since :date until now. The :date field should be again in the format yyyy-MM-dd.    
The :pwd field is a simple password to be in the path, to allow or not the operation.    
I just added the field because the process to fill the database if the most expensive in the app, and I would like to avoid having people querying it.
If your implementation must have a password you probably should implement some hashing here, using something like Buddy.

----------------------------
## Building your own API  
First, you must have a PostgreSQL database. Find the [database.sql](resources/database.sql) file in this project, inside the resources folder.  
You should run those queries to create the necessary tables.

You can keep the api running with lein run or lein run-dev, [Leiningen](https://leiningen.org/) is needed for this project.  
As it is, the port which will serve the endpoint in production is 8888, if you wish to change it go to the [Components](src/coronavirus_scrapper_api/components.clj) namespace, if you need to change it.
To compile it just run **lein uberjar** and them run it as a standard java file, with java -jar, for example, using it behind Nginx, etc.  
In production Java will look for environment variables for the database, DATABASE_USR and DATABASE_PWD, COVID_PWD and also uses the default database name of "postgres".
You must add this values to .bashrc, supervisor, for example. Or add the values to the code directly, before you compile it, into the [Database](src/coronavirus_scrapper_api/components/database.clj) and [utils](src/coronavirus_scrapper_api/utils.clj).
 
--------------------------
### MIT License