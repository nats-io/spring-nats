#!/bin/bash

read -p 'Make sure you have the "Build Admin Signing Key Password" from 1password. Press any key to continue ...'

mvn -N clean install

cd nats-spring-samples
mvn -N clean package install

cd ..

mvn clean package gpg:sign deploy
