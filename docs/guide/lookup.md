# Lookup & Joins

Join collections using MongoDB's `$lookup` stage - similar to SQL joins.

## Simple Lookup (Equality Match)

Join two collections on matching field values:
```kotlin
data class OrderWithCustomer(
    val id: String,
    val total: Double,
    val customerInfo: List<Customer>
)

val results = konduct.collection<Order>()
    .lookup<Customer> {
        from(Customer::class)
        localField(Order::customerId)
        foreignField(Customer::id)
        into("customerInfo")
    }
    .into<OrderWithCustomer>()
    .toList()
```

Result structure:
```kotlin
OrderWithCustomer(
    id = "order123",
    total = 150.0,
    customerInfo = [
        Customer(id = "c1", name = "John Doe", email = "john@example.com")
    ]
)
```

## Unwinding Results

Convert array results to individual documents:
```kotlin
val results = konduct.collection<Order>()
    .lookup<Customer> {
        from(Customer::class)
        localField(Order::customerId)
        foreignField(Customer::id)
        into("customer")
    }
    .unwind("customer")  // Flatten array to object
    .into<Document>()
    .toList()
```

Before unwind:
```kotlin
{ orderId: 1, customer: [{ name: "John" }] }
```

After unwind:
```kotlin
{ orderId: 1, customer: { name: "John" } }
```

### Preserve Null and Empty Arrays

Keep documents even when lookup finds no matches:
```kotlin
konduct.collection<Order>()
    .lookup<Customer> {
        from(Customer::class)
        localField(Order::customerId)
        foreignField(Customer::id)
        into("customer")
    }
    .unwind("customer", preserveNullAndEmptyArrays = true)
    .into<Document>()
    .toList()
```

## Pipeline-Based Lookup

For complex joins with filtering and transformations:
```kotlin
konduct.collection<Order>()
    .lookup<OrderItem> {
        from(OrderItem::class)
        let {
            "orderId" to Order::id
        }
        pipeline {
            match<OrderItem> {
                expr {
                    OrderItem::orderId eq variable("orderId")
                }
            }
            match<OrderItem> {
                OrderItem::quantity gte 5
            }
            sort {
                "quantity" from -1
            }
            limit(10)
        }
        into("items")
    }
    .into<OrderWithItems>()
    .toList()
```

### Available Pipeline Operations

Inside `pipeline { }`:

- `match { }` - Filter joined documents
- `sort { }` - Order results
- `limit(n)` - Limit results
- `skip(n)` - Skip documents
- `project { }` - Select/transform fields

## Using Variables

Access parent document fields in lookup pipeline:
```kotlin
konduct.collection<Product>()
    .lookup<Review> {
        from(Review::class)
        let {
            "productId" to Product::id
            "minRating" to Product::minRating
        }
        pipeline {
            match<Review> {
                expr {
                    and(
                        Review::productId eq variable("productId"),
                        Review::rating gte variable("minRating")
                    )
                }
            }
        }
        into("goodReviews")
    }
    .toList()
```

## Lookup and Merge

Flatten joined fields directly into parent document:
```kotlin
data class OrderWithCustomerInfo(
    val id: String,
    val total: Double,
    val customerName: String,
    val customerEmail: String,
    val customerTier: String
)

val results = konduct.collection<Order>()
    .lookupAndMerge(Customer::class) {
        from()
        on(Order::customerId, Customer::id)
        merge(Customer::name, Customer::email, Customer::tier)
    }
    .into<OrderWithCustomerInfo>()
    .toList()
```

Result (fields merged at top level):
```kotlin
OrderWithCustomerInfo(
    id = "order123",
    total = 150.0,
    customerName = "John Doe",      // From Customer
    customerEmail = "john@example.com",  // From Customer
    customerTier = "gold"           // From Customer
)
```

## Multiple Lookups

Chain multiple lookups:
```kotlin
konduct.collection<Order>()
    .lookup<Customer> {
        from(Customer::class)
        localField(Order::customerId)
        foreignField(Customer::id)
        into("customer")
    }
    .lookup<Product> {
        from(Product::class)
        localField(Order::productId)
        foreignField(Product::id)
        into("product")
    }
    .lookup<Warehouse> {
        from(Warehouse::class)
        localField(Order::warehouseId)
        foreignField(Warehouse::id)
        into("warehouse")
    }
    .into<Document>()
    .toList()
```

## Real-World Examples

### Order with Customer Details
```kotlin
@Service
class OrderService(mongoTemplate: MongoTemplate) {
    private val konduct = Konduct(mongoTemplate)
    
    fun getOrderWithCustomer(orderId: String): OrderDetails? {
        return konduct.collection<Order>()
            .match { Order::id eq orderId }
            .lookup<Customer> {
                from(Customer::class)
                localField(Order::customerId)
                foreignField(Customer::id)
                into("customer")
            }
            .unwind("customer")
            .lookup<OrderItem> {
                from(OrderItem::class)
                let {
                    "orderId" to Order::id
                }
                pipeline {
                    match<OrderItem> {
                        expr {
                            OrderItem::orderId eq variable("orderId")
                        }
                    }
                }
                into("items")
            }
            .into<OrderDetails>()
            .firstOrNull()
    }
}
```

### Product with Recent Reviews
```kotlin
fun getProductWithReviews(productId: String, limit: Int = 5): ProductWithReviews? {
    return konduct.collection<Product>()
        .match { Product::id eq productId }
        .lookup<Review> {
            from(Review::class)
            let {
                "productId" to Product::id
            }
            pipeline {
                match<Review> {
                    expr {
                        Review::productId eq variable("productId")
                    }
                }
                sort {
                    "createdAt" from -1
                }
                limit(limit)
            }
            into("recentReviews")
        }
        .into<ProductWithReviews>()
        .firstOrNull()
}
```

### Customer Purchase History
```kotlin
data class CustomerWithOrders(
    val id: String,
    val name: String,
    val email: String,
    val orders: List<Order>
)

fun getCustomerPurchaseHistory(customerId: String): CustomerWithOrders? {
    return konduct.collection<Customer>()
        .match { Customer::id eq customerId }
        .lookup<Order> {
            from(Order::class)
            let {
                "customerId" to Customer::id
            }
            pipeline {
                match<Order> {
                    expr {
                        Order::customerId eq variable("customerId")
                    }
                }
                sort {
                    "orderDate" from -1
                }
                limit(20)
            }
            into("orders")
        }
        .into<CustomerWithOrders>()
        .firstOrNull()
}
```

## Type Conversion with into()

Convert pipeline result type at any point:
```kotlin
// Convert to Document
.into<Document>()

// Convert to custom type
.into<OrderWithCustomer>()

// Use with KClass
.into(OrderWithCustomer::class)
```

## Performance Tips

1. **Index join fields:**
```kotlin
   @Document("orders")
   data class Order(
       @Indexed val customerId: String  // Index foreign keys
   )
```

2. **Limit joined results:**
```kotlin
   pipeline {
       match<OrderItem> { /* ... */ }
       limit(10)  // Don't fetch thousands
   }
```

3. **Filter early:**
```kotlin
   .match { Order::status eq "active" }  // Before lookup
   .lookup<Customer> { /* ... */ }
```

4. **Use lookupAndMerge for flat structures:**
```kotlin
   // ✅ Better - no nested arrays
   .lookupAndMerge(Customer::class) { /* ... */ }
   
   // ❌ Worse - nested array
   .lookup<Customer> { /* ... */ }
   .unwind("customer")
```

## Common Patterns

### Left Outer Join
```kotlin
konduct.collection<Order>()
    .lookup<Customer> {
        from(Customer::class)
        localField(Order::customerId)
        foreignField(Customer::id)
        into("customer")
    }
    .unwind("customer", preserveNullAndEmptyArrays = true)
    .toList()
```

### Join with Aggregation
```kotlin
konduct.collection<Product>()
    .lookup<Sale> {
        from(Sale::class)
        let {
            "productId" to Product::id
        }
        pipeline {
            match<Sale> {
                expr {
                    Sale::productId eq variable("productId")
                }
            }
            group {
                by { }
                accumulate {
                    "totalSales" sum Sale::amount
                    "salesCount" count Unit
                }
            }
        }
        into("salesStats")
    }
    .toList()
```

### Self-Referencing Lookup
```kotlin
// Find employees and their managers
konduct.collection<Employee>()
    .lookup<Employee> {
        from(Employee::class)
        localField(Employee::managerId)
        foreignField(Employee::id)
        into("manager")
    }
    .unwind("manager", preserveNullAndEmptyArrays = true)
    .toList()
```

## See Also

- [Match & Filter](match.md) - Filter in lookup pipelines
- [Grouping](grouping.md) - Aggregate joined data
- [Expressions](expressions.md) - Use expr for variables