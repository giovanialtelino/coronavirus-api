FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/coronavirus-scrapper-api-0.0.1-SNAPSHOT-standalone.jar /coronavirus-scrapper-api/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/coronavirus-scrapper-api/app.jar"]
