# Custom Stages

Build any MongoDB aggregation stage not yet natively supported.

## Why Custom Stages?

Konduct covers common operations, but MongoDB has 50+ stages. Use custom stages for:

- Stages not yet implemented
- Experimental MongoDB features
- Complex nested pipelines
- Edge cases

## Basic Custom Stage
```kotlin
konduct.collection<Product>()
    .customStage("\$replaceRoot") {
        "newRoot" from "\$embedded.document"
    }
    .toList()
```

## Building Complex Stages

### $redact Stage
```kotlin
konduct.collection<Document>()
    .customStage("\$redact") {
        "\$cond" from {
            "if" from doc {
                "\$eq" from list("\$level", 5)
            }
            "then" from "$$PRUNE"
            "else" from "$$DESCEND"
        }
    }
    .toList()
```

### $graphLookup Stage
```kotlin
konduct.collection<Employee>()
    .customStage("\$graphLookup") {
        "from" from "employees"
        "startWith" from "\$reportsTo"
        "connectFromField" from "reportsTo"
        "connectToField" from "name"
        "as" from "reportingHierarchy"
        "maxDepth" from 3
    }
    .toList()
```

### $bucket Stage
```kotlin
konduct.collection<Product>()
    .customStage("\$bucket") {
        "groupBy" from "\$price"
        "boundaries" from list(0, 100, 200, 300, 500, 1000)
        "default" from "Other"
        "output" from {
            "count" from doc { "\$sum" from 1 }
            "titles" from doc { "\$push" from "\$title" }
            "avgPrice" from doc { "\$avg" from "\$price" }
        }
    }
    .toList()
```

### $setWindowFields Stage
```kotlin
konduct.collection<Sale>()
    .customStage("\$setWindowFields") {
        "partitionBy" from "\$category"
        "sortBy" from { "date" from 1 }
        "output" from {
            "cumulativeTotal" from {
                "\$sum" from "\$amount"
                "window" from {
                    "documents" from list("unbounded", "current")
                }
            }
            "rank" from {
                "\$rank" from doc { }
            }
        }
    }
    .toList()
```

## Helper Functions

### Build Documents
```kotlin
.customStage("\$lookup") {
    "from" from "orders"
    "let" from {
        "userId" from "\$_id"
    }
    "pipeline" from list(
        doc {
            "\$match" from {
                "\$expr" from {
                    "\$eq" from list("\$userId", "$$userId")
                }
            }
        },
        doc {
            "\$sort" from { "orderDate" from -1 }
        }
    )
    "as" from "orders"
}
```

### Build Lists
```kotlin
"boundaries" from list(0, 100, 200, 300)
"values" from list("\$field1", "\$field2", "\$field3")
```

## Real-World Examples

### Geospatial $geoNear
```kotlin
konduct.collection<Store>()
    .customStage("\$geoNear") {
        "near" from {
            "type" from "Point"
            "coordinates" from list(-73.9667, 40.78)
        }
        "distanceField" from "distance"
        "maxDistance" from 5000
        "spherical" from true
    }
    .limit(10)
    .toList()
```

### Text Search $search
```kotlin
konduct.collection<Article>()
    .customStage("\$search") {
        "index" from "article_index"
        "text" from {
            "query" from "mongodb aggregation"
            "path" from list("title", "content")
        }
    }
    .limit(20)
    .toList()
```

### $unionWith
```kotlin
konduct.collection<Product>()
    .customStage("\$unionWith") {
        "coll" from "archived_products"
        "pipeline" from list(
            doc {
                "\$match" from { "status" from "archived" }
            }
        )
    }
    .toList()
```

### $merge (Output to Collection)
```kotlin
konduct.collection<Sale>()
    .group {
        by(Sale::productId)
        accumulate {
            "totalSales" sum Sale::amount
        }
    }
    .customStage("\$merge") {
        "into" from "product_statistics"
        "on" from "_id"
        "whenMatched" from "replace"
        "whenNotMatched" from "insert"
    }
    .toList()
```

## Type Safety

Custom stages return `Document` by default. Convert with `into()`:
```kotlin
data class GeoResult(
    val name: String,
    val distance: Double
)

konduct.collection<Store>()
    .customStage("\$geoNear") {
        "near" from coordinates
        "distanceField" from "distance"
    }
    .into<GeoResult>()
    .toList()
```

## Debugging Custom Stages
```kotlin
val pipeline = konduct.collection<Product>()
    .customStage("\$bucket") {
        // ... complex stage
    }

println(pipeline.toJson())
// Inspect generated MongoDB JSON
```

## When to Use Custom Stages

✅ **Use custom stages when:**

- Feature not yet in Konduct
- Complex MongoDB-specific operation
- Experimental/new MongoDB features

❌ **Don't use custom stages when:**

- Native Konduct method exists (use it instead!)
- Can be built with existing methods
- Just learning MongoDB (use native methods first)

## Contributing

Found yourself using the same custom stage repeatedly? Consider contributing it to Konduct! See [Contributing](../contributing.md).

## See Also

- [MongoDB Aggregation Stages](https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/) - Full stage reference
- [API Reference](../api/index.html) - CustomStageBuilder docs