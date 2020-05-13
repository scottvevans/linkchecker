# linkchecker

## A simple reactive link checker (web crawler) application based on:
1. Java 11
2. Spring Boot 2.2.7 
3. Spring WebFlux 2.2.7 (for non-blocking retrieval of pages)
4. Lombok
5. Junit
6. Jsoup for html parsing
7. Swagger for api documentation

---

## To run docker image
1. docker pull scottvevans/linkchecker:latest
2. docker run -p 8080:8080 scottvevans/linkchecker:latest

*Prerequisite required to run: [docker](https://www.docker.com/get-started)

---

## To build, run and test
1. git clone https://github.com/scottvevans/linkchecker.git
2. cd linkchecker
3. ./docker.sh
4. open http://localhost:8080/swagger-ui.html

*Prerequisites required to run: [maven](https://maven.apache.org/download.cgi) and [docker](https://www.docker.com/get-started)

---

### @TODO
1. add more unit tests
2. add integration tests
3. cleanup spurious log messages







