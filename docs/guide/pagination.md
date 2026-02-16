# Pagination

Efficiently paginate large result sets with built-in support.

## Basic Pagination
```kotlin
val result: PagedResult<Product>? = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .sort { Product::name.asc() }
    .paginate(page = 0, pageSize = 20)
    .firstOrNull()
```

## PagedResult Structure
```kotlin
data class PagedResult<T>(
    val data: List<T>,        // Current page items
    val total: Long,          // Total matching documents
    val page: Int,            // Current page (0-based)
    val pageSize: Int,        // Items per page
    val totalPages: Int       // Total number of pages
)
```

## Accessing Results
```kotlin
result?.let {
    println("Showing ${it.data.size} of ${it.total} products")
    println("Page ${it.page + 1} of ${it.totalPages}")
    
    it.data.forEach { product ->
        println(product.name)
    }
}
```

## With Filters and Sorting
```kotlin
fun searchProducts(
    query: String,
    category: String?,
    page: Int,
    pageSize: Int
): PagedResult<Product> {
    return konduct.collection<Product>()
        .match {
            Product::name regex query.toRegex(RegexOption.IGNORE_CASE)
            Product::status eq "active"
            category?.let { Product::category eq it }
        }
        .sort { Product::rating.desc() }
        .paginate(page, pageSize)
        .firstOrNull() ?: PagedResult(
            data = emptyList(),
            total = 0,
            page = page,
            pageSize = pageSize,
            totalPages = 0
        )
}
```

## REST API Example
```kotlin
@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {
    
    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) category: String?
    ): PagedResult<Product> {
        return productService.getProducts(page, size, category)
    }
}
```

Response:
```json
{
  "data": [
    { "id": "1", "name": "Product 1", "price": 99.99 },
    { "id": "2", "name": "Product 2", "price": 149.99 }
  ],
  "total": 150,
  "page": 0,
  "pageSize": 20,
  "totalPages": 8
}
```

## Paginating Grouped Results
```kotlin
val result = konduct.collection<Order>()
    .match { Order::status eq "completed" }
    .group {
        by(Order::customerId)
        accumulate {
            "totalSpent" sum Order::total
            "orderCount" count Unit
        }
    }
    .sort { "totalSpent".desc() }
    .paginate(page = 0, pageSize = 10)
    .firstOrNull()
```

## Performance Considerations

### DO: Filter Before Paginating
```kotlin
// ✅ Good - filter first
konduct.collection<Product>()
    .match { Product::category eq "Electronics" }  // Reduces dataset
    .paginate(page, pageSize)
```

### DON'T: Paginate Everything
```kotlin
// ❌ Bad - paginating millions of documents
konduct.collection<Product>()
    .paginate(page, pageSize)  // No filter!
```

### Use Indexes
```kotlin
@Document
@CompoundIndex(def = "{'status': 1, 'createdAt': -1}")
data class Product(
    @Indexed val status: String,
    val createdAt: Date
)
```

## Manual Pagination

If you need custom pagination:
```kotlin
konduct.collection<Product>()
    .match { Product::status eq "active" }
    .sort { Product::createdAt.desc() }
    .skip(page * pageSize)
    .limit(pageSize)
    .toList()

// Get total separately
val total = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .count()
```

## Cursor-Based Pagination (Alternative)

For very large datasets, use cursor-based pagination:
```kotlin
fun getProductsAfter(
    lastId: String?,
    limit: Int = 20
): List<Product> {
    return konduct.collection<Product>()
        .match {
            Product::status eq "active"
            lastId?.let { Product::id gt it }
        }
        .sort { Product::id.asc() }
        .limit(limit)
        .toList()
}
```

## See Also

- [Match & Filter](match.md) - Filter before paginating
- [Sorting](sorting.md) - Order results
- [Performance](../advanced/performance.md) - Optimize queries