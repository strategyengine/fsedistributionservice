# strategyengine token distribution rest service

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


#To start: Run FseDistributionServiceApplication and navigate to the swagger page and you can call the endpoints directly
http://localhost:8080


#Good luck!
