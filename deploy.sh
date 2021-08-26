mvn clean compile install
docker build -t assetpriceservice .
docker tag assetpriceservice gcr.io/flarestrategyengine/assetpriceservice
docker push gcr.io/flarestrategyengine/assetpriceservice