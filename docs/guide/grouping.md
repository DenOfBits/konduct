# Grouping & Aggregation

Group documents and compute aggregated values.

## Simple Grouping

Group by a single field:
```kotlin
konduct.collection<Order>()
    .group {
        by(Order::status)
        accumulate {
            "count" count Unit
            "totalAmount" sum Order::amount
        }
    }
    .toList()
```

Result structure:
```kotlin
{
    _id: "completed",
    status: "completed",  // Auto-added
    count: 150,
    totalAmount: 45000.0
}
```

## Composite Key Grouping

Group by multiple fields:
```kotlin
konduct.collection<Sale>()
    .group {
        by {
            "category" from Sale::category
            "region" from Sale::region
        }
        accumulate {
            "totalSales" sum Sale::amount
            "avgSale" avg Sale::amount
        }
    }
    .toList()
```

Result:
```kotlin
{
    _id: { category: "Electronics", region: "North" },
    category: "Electronics",  // Auto-added
    region: "North",          // Auto-added
    totalSales: 125000.0,
    avgSale: 450.0
}
```

## Time-Based Grouping

Group by time units:
```kotlin
konduct.collection<Sale>()
    .group {
        by(Sale::date, unit = TimeUnit.MONTH)
        accumulate {
            "monthlySales" sum Sale::amount
            "orderCount" count Unit
        }
    }
    .toList()
```

## Expression-Based Aggregation

Use expressions in accumulators:
```kotlin
konduct.collection<OrderItem>()
    .group {
        by(OrderItem::orderId)
        accumulate {
            "totalRevenue" sum (OrderItem::quantity * OrderItem::price)
            "totalDiscount" sum ((OrderItem::originalPrice - OrderItem::salePrice) * OrderItem::quantity)
            "itemCount" count Unit
        }
    }
    .toList()
```

## All Accumulators

### Numeric Aggregations
```kotlin
accumulate {
    "total" sum Product::price
    "average" avg Product::price
    "minimum" min Product::price
    "maximum" max Product::price
}
```

### Counting
```kotlin
accumulate {
    "count" count Unit
    "uniqueCustomers" countDistinct Order::customerId
}
```

### Array Accumulators
```kotlin
accumulate {
    "allNames" push Product::name
    "uniqueTags" addToSet Product::tags
}
```

### First & Last
```kotlin
konduct.collection<Order>()
    .sort { Order::orderDate.asc() }
    .group {
        by(Order::customerId)
        accumulate {
            "firstOrder" first Order::orderDate
            "lastOrder" last Order::orderDate
        }
    }
    .toList()
```

## Typed Results

Get type-safe results:
```kotlin
data class CategoryStats(
    val _id: String,
    val category: String,
    val count: Int,
    val avgPrice: Double,
    val totalRevenue: Double
)

val results: List<CategoryStats> = konduct.collection<Product>()
    .group<Product, CategoryStats> {
        by(Product::category)
        accumulate {
            "count" count Unit
            "avgPrice" avg Product::price
            "totalRevenue" sum Product::price
        }
    }
    .toList()
```

Or use `into()`:
```kotlin
val results = konduct.collection<Product>()
    .group {
        by(Product::category)
        accumulate {
            "count" count Unit
            "avgPrice" avg Product::price
        }
    }
    .into<CategoryStats>()
    .toList()
```

## Real-World Examples

### Customer Lifetime Value
```kotlin
konduct.collection<Order>()
    .match { Order::status eq "completed" }
    .group {
        by(Order::customerId)
        accumulate {
            "totalSpent" sum Order::total
            "orderCount" count Unit
            "avgOrderValue" avg Order::total
            "firstOrderDate" min Order::orderDate
            "lastOrderDate" max Order::orderDate
        }
    }
    .match { "totalSpent" gte 1000 }
    .sort { "totalSpent".desc() }
    .into<CustomerLTV>()
    .toList()
```

### Sales by Category and Month
```kotlin
konduct.collection<Sale>()
    .group {
        by {
            "category" from Sale::category
            "month" from Sale::date.month()
            "year" from Sale::date.year()
        }
        accumulate {
            "revenue" sum (Sale::quantity * Sale::price)
            "units" sum Sale::quantity
            "transactions" count Unit
        }
    }
    .sort {
        "year".desc()
        "month".desc()
        "revenue".desc()
    }
    .toList()
```

## See Also

- [Expressions](expressions.md) - Use operators in aggregations
- [Pagination](pagination.md) - Paginate grouped results
- [API Reference](../api/index.html) - Complete API docs