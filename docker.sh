#!/bin/bash

set -e

#1 build the app with maven
mvn package

#2 build the docker image
docker build -t scottvevans/linkchecker .

#3 runs the docker image
docker run -p 8080:8080 scottvevans/linkchecker
