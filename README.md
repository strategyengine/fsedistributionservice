# strategyengine token distribution rest service

### Reference Documentation
This spring boot application provides endpoints for sending trustline tokens

#GET /api/accountinfo/{classicAddress}
Get XRP account info

#POST /api/payment
send payment from a specific address to another

#POST /api/payment/trustlines
send payments to all addresses configured for a trustline.    (This is the one you are looking for)

#GET /api/trustlines/{classicAddress}
get trustlines for an address


#To start: Run FseDistributionServiceApplication and navigate to the swagger page and you can call the endpoints directly
http://localhost:8080


#Good luck!
