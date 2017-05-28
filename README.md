# Auction service


## Create auction with predefined ID
```
curl -vvv -X POST \
-H "Content-type: application/json" \
-d '{"startingPrice": 1.00, "durationInSeconds":60, "title":"test1", "auctionId":"08c2266f-79d9-435a-befe-f149d796a615"}' \
http://localhost:9000/auctions/2ec879bd-6dc2-4906-a0d2-82a413a9dcdb
```

## Bid on auction
```
curl -vvv -X POST \
-H "Content-type: application/json" \
-d '{"price":2.00, "userId": "23384adb-b7a8-4951-9019-963b7f45a6af"}' \
http://localhost:9000/auctions/08c2266f-79d9-435a-befe-f149d796a615/bid
```