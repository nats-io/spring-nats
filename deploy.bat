@echo --------------------------------------------------------------------------------
@echo 1. Make sure you have the following from 1password
@echo "Java GHA SIGNING_PASSWORD Build Admin" - used for gpg signing
@echo "spring-nats.settings.xml" - used to store sonatype login and gpgp references
@echo --------------------------------------------------------------------------------
@echo 2. You must have Java 17 or later
@echo --------------------------------------------------------------------------------
pause

call mvn -N clean install

cd nats-spring-samples
call mvn -N clean package install

cd ..

call mvn clean package gpg:sign deploy
