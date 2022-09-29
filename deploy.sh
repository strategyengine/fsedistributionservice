docker rmi fsedistributionservice:latest
rm ../fsedistributionservice.tar.gz
mvn clean compile install
docker build -t fsedistributionservice:latest .
docker save fsedistributionservice:latest | gzip > ../fsedistributionservice.tar.gz
pscp -pw 1359 ../fsedistributionservice.tar.gz  pops@10.0.0.33:/home/pops/docker/deploys
#pscp -pw 1359 ../fsedistributionservice.tar.gz  pops@10.0.0.31:/home/pops/docker/deploys

