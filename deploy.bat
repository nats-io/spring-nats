call mvn -N clean install

cd nats-spring-samples
call mvn -N clean package install

cd ..

call mvn clean package gpg:sign deploy
