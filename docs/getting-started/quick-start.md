# Quick Start

Get up and running with Konduct in 5 minutes!

## Step 1: Define Your Model
```kotlin
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("products")
data class Product(
    @Id val id: String? = null,
    val name: String,
    val price: Double,
    val category: String,
    val status: String,
    val rating: Double? = null
)
```

## Step 2: Create a Service
```kotlin
import io.github.denofbits.konduct.core.Konduct
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class ProductService(mongoTemplate: MongoTemplate) {
    
    private val konduct = Konduct(mongoTemplate)
    
    fun getActiveProducts(): List<Product> {
        return konduct.collection<Product>()
            .match { Product::status eq "active" }
            .sort { Product::price.asc() }
            .toList()
    }
}
```

## Step 3: Use It!
```kotlin
@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {
    
    @GetMapping
    fun getProducts(): List<Product> {
        return productService.getActiveProducts()
    }
}
```

## Common Patterns

### Filtering
```kotlin
konduct.collection<Product>()
    .match {
        Product::status eq "active"
        Product::price gte 100
        Product::category `in` listOf("Electronics", "Books")
    }
    .toList()
```

### Sorting
```kotlin
konduct.collection<Product>()
    .sort {
        Product::category.asc()
        Product::price.desc()
    }
    .toList()
```

### Pagination
```kotlin
val result = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .paginate(page = 0, pageSize = 20)
    .firstOrNull()

println("Total: ${result?.total}")
println("Items: ${result?.data?.size}")
```

### Grouping
```kotlin
konduct.collection<Order>()
    .group {
        by(Order::customerId)
        accumulate {
            "totalSpent" sum Order::amount
            "orderCount" count Unit
            "avgOrder" avg Order::amount
        }
    }
    .toList()
```

## Next Steps

- [Core Concepts](../guide/core-concepts.md) - Understand the fundamentals
- [Match & Filter](../guide/match.md) - Master filtering
- [Grouping](../guide/grouping.md) - Aggregate your data