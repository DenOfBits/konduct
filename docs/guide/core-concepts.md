# Core Concepts

Understanding Konduct's fundamental principles.

## The Pipeline Pattern

Aggregation pipelines transform data through stages:
```
Documents → Match → Group → Sort → Results
   1000   →  500  →  50   →  50  →   50
```

Each method adds a stage:
```kotlin
konduct.collection<Product>()
    .match { /* Stage 1 */ }
    .group { /* Stage 2 */ }
    .sort { /* Stage 3 */ }
    .toList()  // Execute
```

## Immutability

Every operation returns a **new** pipeline:
```kotlin
val base = konduct.collection<Product>()
    .match { Product::status eq "active" }

// Two different pipelines from same base
val electronics = base.match { Product::category eq "electronics" }
val books = base.match { Product::category eq "books" }

// 'base' is unchanged
```

## Type Safety

Use Kotlin property references for compile-time safety:
```kotlin
// ✅ Type-safe - compiler checks field exists
Product::name eq "Widget"
Product::price gte 100

// ✅ IDE autocomplete works
Product::  // Shows all fields

// ❌ Compile error if field doesn't exist
Product::invalidField  // Won't compile

// String fallback for dynamic fields
"customField_${userId}" eq "value"
```

## Field Name Mapping

Konduct respects `@Field` annotations:
```kotlin
import org.springframework.data.mongodb.core.mapping.Field

data class Product(
    @Id val id: String?,              // → "_id" in MongoDB
    @Field("product_name") val name: String,  // → "product_name"
    val price: Double                  // → "price"
)

// All work correctly:
Product::id eq "123"       // Uses "_id"
Product::name eq "Widget"  // Uses "product_name"
Product::price gte 100     // Uses "price"
```

## Execution Model

Pipelines are **lazy** - they don't execute until you call a terminal operation:
```kotlin
val pipeline = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .sort { Product::price.asc() }
// Nothing executed yet!

// Execute now:
val results = pipeline.toList()  // Sends to MongoDB
```

Terminal operations:

- `toList()` - Get all results
- `firstOrNull()` - Get first or null
- `count()` - Count matching documents

## Building Blocks

### Match (Filter)
```kotlin
.match {
    Product::status eq "active"
    Product::price gte 100
}
```

### Sort
```kotlin
.sort {
    Product::category.asc()
    Product::price.desc()
}
```

### Group (Aggregate)
```kotlin
.group {
    by(Product::category)
    accumulate {
        "count" count Unit
        "avgPrice" avg Product::price
    }
}
```

### Limit & Skip
```kotlin
.skip(20)
.limit(10)
```

### Add Fields
```kotlin
.addFields {
    "total" from (Order::quantity * Order::price)
}
```

## Type Conversion

Change pipeline type with `into()`:
```kotlin
data class CategoryStats(
    val _id: String,
    val count: Int,
    val avgPrice: Double
)

konduct.collection<Product>()
    .group {
        by(Product::category)
        accumulate {
            "count" count Unit
            "avgPrice" avg Product::price
        }
    }
    .into<CategoryStats>()  // Convert type
    .toList()  // Returns List<CategoryStats>
```

## Common Patterns

### Filter → Sort → Limit
```kotlin
konduct.collection<Product>()
    .match { Product::inStock eq true }
    .sort { Product::rating.desc() }
    .limit(10)
    .toList()
```

### Group → Sort → Paginate
```kotlin
konduct.collection<Order>()
    .group {
        by(Order::customerId)
        accumulate { "total" sum Order::amount }
    }
    .sort { "total".desc() }
    .skip(page * pageSize)
    .limit(pageSize)
    .toList()
```

### Match → Group → Match

Filter aggregated results:
```kotlin
konduct.collection<Sale>()
    .match { Sale::date gte lastMonth }  // Filter input
    .group {
        by(Sale::productId)
        accumulate { "revenue" sum Sale::amount }
    }
    .match { "revenue" gte 10000 }  // Filter aggregated results
    .toList()
```

## Debugging

### View Generated Pipeline
```kotlin
val pipeline = konduct.collection<Product>()
    .match { Product::status eq "active" }

println(pipeline.toJson())
// Shows MongoDB aggregation JSON
```

### Get Raw Aggregation
```kotlin
val aggregation = pipeline.toAggregation()
// Returns Spring Data Aggregation object
```

## Next Steps

- [Match & Filter](match.md) - Master filtering
- [Grouping](grouping.md) - Aggregate data
- [Expressions](expressions.md) - Use operators