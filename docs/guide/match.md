# Match & Filter

Filter documents using the `$match` stage - MongoDB's equivalent of SQL's WHERE clause.

## Basic Equality
```kotlin
konduct.collection<Product>()
    .match {
        Product::status eq "active"
        Product::category eq "Electronics"
    }
    .toList()
```

Multiple conditions are combined with AND by default.

## Comparison Operators

### Greater Than / Less Than
```kotlin
.match {
    Product::price gt 100        // Greater than
    Product::price gte 100       // Greater than or equal
    Product::stock lt 10         // Less than
    Product::rating lte 3.5      // Less than or equal
}
```

### Not Equal
```kotlin
.match {
    Product::status ne "deleted"
}
```

## Set Operators

### In / Not In
```kotlin
.match {
    Product::category `in` listOf("Electronics", "Books", "Toys")
    Product::status nin listOf("deleted", "archived")
}
```

## String Operations

### Regex
```kotlin
.match {
    Product::name regex "iPhone.*"
    Product::description regex "wireless".toRegex(RegexOption.IGNORE_CASE)
}
```

## Null Checks
```kotlin
.match {
    Product::deletedAt.isNull()
    Product::archivedAt.isNotNull()
    Product::rating exists true
}
```

## Logical Operators

### Implicit AND
```kotlin
.match {
    Product::status eq "active"
    Product::price gte 100
    // Both must be true
}
```

### OR
```kotlin
.match {
    or(
        Product::featured eq true,
        Product::discount gt 0.3
    )
}
```

### NOT
```kotlin
.match {
    not(Product::deleted eq true)
}
```

## Real-World Examples

### Find High-Value Orders
```kotlin
fun findHighValueOrders(): List<Order> {
    return konduct.collection<Order>()
        .match {
            Order::status eq "completed"
            Order::total gte 1000
            Order::orderDate gte thirtyDaysAgo()
        }
        .sort { Order::total.desc() }
        .toList()
}
```

### Search Products
```kotlin
fun searchProducts(query: String, minPrice: Double, maxPrice: Double): List<Product> {
    return konduct.collection<Product>()
        .match {
            Product::name regex query.toRegex(RegexOption.IGNORE_CASE)
            Product::status eq "active"
            Product::price gte minPrice
            Product::price lte maxPrice
        }
        .sort { Product::rating.desc() }
        .toList()
}
```

### Find Expiring Subscriptions
```kotlin
fun findExpiringSubscriptions(days: Int): List<Subscription> {
    val today = Date()
    val futureDate = Date(today.time + days * 24 * 60 * 60 * 1000)
    
    return konduct.collection<Subscription>()
        .match {
            Subscription::status eq "active"
            Subscription::expiryDate gte today
            Subscription::expiryDate lte futureDate
            Subscription::autoRenew eq false
        }
        .sort { Subscription::expiryDate.asc() }
        .toList()
}
```

## String-Based Fallback

For dynamic field names:
```kotlin
.match {
    "customField_${userId}" eq "value"
    "metadata.createdBy" eq userId
}
```

## Performance Tips

1. **Index matched fields:**
```kotlin
   @Indexed
   data class Product(
       @Indexed val status: String,
       @Indexed val category: String
   )
```

2. **Filter early:**
```kotlin
   // ✅ Good - filter before expensive operations
   .match { Product::status eq "active" }
   .lookup { /* ... */ }
   
   // ❌ Bad - filter after expensive operations
   .lookup { /* ... */ }
   .match { Product::status eq "active" }
```

3. **Use selective filters:**
```kotlin
   // ✅ Good - narrows results significantly
   .match {
       Product::category eq "Electronics"
       Product::price gte 1000
   }
```

## See Also

- [Sorting](sorting.md) - Order results
- [Grouping](grouping.md) - Aggregate data
- [API Reference](../api/index.html) - Complete match API