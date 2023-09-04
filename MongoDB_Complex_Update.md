```
var productSeasonalityUpdateList = [];
db.productSeasonalityList.find({productId:  { $in: ["000000000040000303","000000000030000141","000000000022563818","000000000022550218"] } }).forEach(function(doc){ 
        print('For productId :: '+doc.productId)
        productSeasonalityUpdateList.push({
        "updateOne": {
            "filter": { "_id": doc._id },
            "update": { "$set": { 
                                    "productLaunchDate": doc.productLaunchDate.replaceAll('-','/') ,
                                    "productEndDate": doc.productEndDate.replaceAll('-','/')
                                } }
        }
    });
    
    if ( productSeasonalityUpdateList.length === 500 ) {
        print(' Executing batch of 500 documents Bulk Update');
        db.productSeasonalityList.bulkWrite(productSeasonalityUpdateList);
        productSeasonalityUpdateList = [];
        print('Crossed 500 , so batch update')
    }
})

if ( productSeasonalityUpdateList.length > 0 )  {
    print(' Under 500 documents , so executing Bulk Update');
    db.productSeasonalityList.bulkWrite(productSeasonalityUpdateList);
}
```
