# strategyengine token distribution rest service

#How to run locally
1. Install https://jdk.java.net/java-se-ri/11 
2. Install maven https://maven.apache.org/download.cgi 
3. install docker https://docs.docker.com/get-docker/
4. mvn clean compile install
5. docker build -t fsedistributionservice .
6. docker run -p 127.0.0.1:8080:8080 fsedistributionservice


### Reference Documentation
This spring boot application provides endpoints for sending trustline tokens

#GET /api/accountinfo/{classicAddress}
Get XRP account info

#POST /api/payment
send payment from a specific address to another

#POST /api/payment/trustlines
send payments to all addresses configured for a trustline. 

#POST /api/payment/trustlines/min/airdrop
schedule an airdrop to send tokens after a min number of trustlines exist.   "Airdrop begins once 3000 trustlines created"

* minTrustLinesTriggerValue: once you have this many trustlines, it's going to send
* amount: how much to send
* example POST JSON
*  {
  "minTrustLinesTriggerValue": 3000,
  "trustlinePaymentRequest": {
      "amount": "5",  
      "currencyName": "FSE",
      "fromClassicAddress": "r........",
      "fromPrivateKey": "ED....",
      "fromSigningPublicKey": "ED.....",
      "trustlineIssuerClassicAddress": "r...."
  }
} 

#GET /api/trustlines/{classicAddress}
get trustlines for an address


#To start: 
* Run FseDistributionServiceApplication
* Navigate to the swagger page and you can call the endpoints directly  http://localhost:8080
* Click the XRPL Trustline endpoints section
* Click POST /api/payment
* Click Model where it says Model|Schema for documentation on each parameter 

#Good luck!


* Feeling generous?  Throw a spare coin this way: rNP3mFp8QGe1yXYMdypU7B5rXM1HK9VbDK


* ![Thank you](qr_trusty.png?img_id=9&sbid=140421&w=300)




#Big Query Dependencies
        CREATE OR REPLACE TABLE `flarestrategyengine.strategyengine.zero_balance_payment`
                  (
                  requestDate TIMESTAMP NOT NULL,
                  toAddress String,
                  amount String,
                  currency String,
                  issuer String,
                  fromAddress String
                  )
                  PARTITION BY DATE(requestDate) 


Add environment variable
GOOGLE_APPLICATION_CREDENTIALS=<path_to_api_key>\gcp-svc-act-flarestrategyengine-050db9777d59.json