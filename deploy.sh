mvn clean compile install
docker build -t fsedistributionservice .
docker tag fsedistributionservice gcr.io/flarestrategyengine/fsedistributionservice
docker push gcr.io/flarestrategyengine/fsedistributionservice