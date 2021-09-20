#!/bin/bash

mvn -N clean install

cd nats-spring-samples
mvn -N clean package install

cd ..

mvn clean package gpg:sign deploy
