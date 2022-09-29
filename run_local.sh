docker rmi fsedistributionservice:latest
rm ../fsedistributionservice.tar.gz
mvn clean compile install
docker build -t fsedistributionservice:latest .
docker compose up

