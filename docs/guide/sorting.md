# Sorting

Order your results using the `$sort` stage.

## Basic Sorting

### Single Field
```kotlin
// Ascending
konduct.collection<Product>()
    .sort { Product::price.asc() }
    .toList()

// Descending
konduct.collection<Product>()
    .sort { Product::rating.desc() }
    .toList()
```

### Multiple Fields
```kotlin
konduct.collection<Product>()
    .sort {
        Product::category.asc()
        Product::price.desc()
    }
    .toList()
```

Sorting order matters - sorts by category first, then by price within each category.

## String-Based Sorting

For dynamic or nested fields:
```kotlin
konduct.collection<Product>()
    .sort {
        "metadata.createdAt".desc()
        "customField".asc()
    }
    .toList()
```

## Helper Methods

Sort multiple fields at once:
```kotlin
konduct.collection<Product>()
    .sort {
        ascending(Product::category, Product::name)
    }
    .toList()

konduct.collection<Product>()
    .sort {
        descending(Product::rating, Product::reviewCount)
    }
    .toList()
```

## Real-World Examples

### Top Rated Products
```kotlin
fun getTopRatedProducts(limit: Int = 10): List<Product> {
    return konduct.collection<Product>()
        .match { Product::status eq "active" }
        .sort { Product::rating.desc() }
        .limit(limit)
        .toList()
}
```

### Recent Orders
```kotlin
fun getRecentOrders(customerId: String): List<Order> {
    return konduct.collection<Order>()
        .match { Order::customerId eq customerId }
        .sort { Order::orderDate.desc() }
        .limit(20)
        .toList()
}
```

### Alphabetical with Price
```kotlin
fun browseProducts(category: String): List<Product> {
    return konduct.collection<Product>()
        .match {
            Product::category eq category
            Product::status eq "active"
        }
        .sort {
            Product::name.asc()
            Product::price.asc()
        }
        .toList()
}
```

## Sorting Grouped Results

Sort after aggregation using string field names:
```kotlin
konduct.collection<Order>()
    .group {
        by(Order::customerId)
        accumulate {
            "totalSpent" sum Order::total
            "orderCount" count Unit
        }
    }
    .sort {
        "totalSpent".desc()
        "orderCount".desc()
    }
    .toList()
```

## Performance Tips

1. **Use indexes:**
```kotlin
   @Document
   @CompoundIndex(def = "{'category': 1, 'price': -1}")
   data class Product(...)
```

2. **Limit before sorting when possible:**
```kotlin
   // ✅ Good - if filter is selective
   .match { Product::featured eq true }  // Only 50 docs
   .sort { Product::price.desc() }
   
   // ❌ Bad - sorting millions
   .sort { Product::price.desc() }
   .match { Product::featured eq true }
```

3. **Sort direction in index:**
   MongoDB can use indexes more efficiently when sort direction matches index direction.

## See Also

- [Match & Filter](match.md) - Filter before sorting
- [Pagination](pagination.md) - Combine with skip/limit
- [Performance](../advanced/performance.md) - Optimize sorting