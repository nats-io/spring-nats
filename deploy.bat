@echo Make sure you have the "Build Admin Signing Key Password" from 1password
pause

call mvn -N clean install

cd nats-spring-samples
call mvn -N clean package install

cd ..

call mvn clean package gpg:sign deploy
