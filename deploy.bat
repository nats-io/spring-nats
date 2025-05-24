@echo --------------------------------------------------------------------------------
@echo 1. Make sure you have the following from 1password
@echo "Java OSSRH CENTRAL USERNAME and TOKEN nats.io synadia.io" - used for gpg signing
@echo "settings.xml" - populate the environment
@echo --------------------------------------------------------------------------------
@echo 2. You must have Java 17 or later
@echo --------------------------------------------------------------------------------
pause

call mvn --settings settings.xml -N clean install

cd nats-spring-samples
call mvn --settings ..\settings.xml -N clean package install
cd ..

call mvn --settings settings.xml clean package sign:sign deploy
