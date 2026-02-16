# Add Fields

Add computed fields to documents using `$addFields` stage.

## Basic Usage

Add new fields to documents:
```kotlin
konduct.collection<Product>()
    .addFields {
        "totalValue" from (Product::stock * Product::price)
        "inStock" from (Product::quantity gt 0)
    }
    .toList()
```

## Field Sources

### From Property References
```kotlin
.addFields {
    "productName" from Product::name
    "cost" from Product::price
}
```

### From String Fields
```kotlin
.addFields {
    "uppercaseName" from "\$name"
    "nestedField" from "\$metadata.createdBy"
}
```

### From Literal Values
```kotlin
.addFields {
    "status" from "pending"
    "processedAt" from Date()
    "version" from 1
}
```

### From Expressions
```kotlin
.addFields {
    "total" from (Order::quantity * Order::price)
    "profit" from ((Product::sellingPrice - Product::costPrice) * Product::stock)
    "discountedPrice" from (Product::price * (1 - (Product::discount / 100)))
}
```

## Array Operations

### Sum Array Elements
```kotlin
.addFields {
    "totalHomework" sumOf Student::homework
    "totalQuiz" sumOf Student::quiz
}
```

### Average Array
```kotlin
.addFields {
    "avgScore" avgOf Student::scores
}
```

### Min/Max Array
```kotlin
.addFields {
    "highestScore" maxOf Student::scores
    "lowestScore" minOf Student::scores
}
```

### Array Size
```kotlin
.addFields {
    "homeworkCount" sizeOf Student::homework
    "tagCount" sizeOf Product::tags
}
```

## String Concatenation

Combine strings:
```kotlin
.addFields {
    "fullName" concat listOf(User::firstName, " ", User::lastName)
    "displayName" concat listOf(User::title, ". ", User::name)
}
```

## Conditional Fields

Add fields based on conditions:
```kotlin
.addFields {
    "stockStatus" from when {
        Product::stock eq 0 -> "out_of_stock"
        Product::stock lt Product::reorderPoint -> "low_stock"
        otherwise -> "in_stock"
    }
}
```

## Real-World Examples

### E-Commerce: Calculate Order Total
```kotlin
data class OrderWithTotal(
    val id: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double
)

konduct.collection<Order>()
    .addFields {
        "subtotal" sumOf "items.price"
        "tax" from ("\$subtotal" * 0.1)
        "total" from ("\$subtotal" + "\$tax")
    }
    .into<OrderWithTotal>()
    .toList()
```

### Inventory: Stock Value
```kotlin
konduct.collection<Product>()
    .addFields {
        "stockValue" from (Product::quantity * Product::costPrice)
        "potentialRevenue" from (Product::quantity * Product::sellingPrice)
        "potentialProfit" from (
            (Product::quantity * Product::sellingPrice) - 
            (Product::quantity * Product::costPrice)
        )
        "marginPercentage" from (
            ((Product::sellingPrice - Product::costPrice) / Product::sellingPrice) * 100
        )
    }
    .match { "stockValue" gte 10000 }
    .sort { "potentialProfit".desc() }
    .toList()
```

### User Profile: Full Name and Age
```kotlin
konduct.collection<User>()
    .addFields {
        "fullName" concat listOf(User::firstName, " ", User::lastName)
        "age" from yearsDiff(User::birthDate, Date())
        "isAdult" from (yearsDiff(User::birthDate, Date()) gte 18)
    }
    .toList()
```

### Sales: Commission Calculation
```kotlin
konduct.collection<Sale>()
    .addFields {
        "revenue" from (Sale::quantity * Sale::price)
        "commission" from (
            (Sale::quantity * Sale::price) * (Sale::commissionRate / 100)
        )
        "netRevenue" from (
            (Sale::quantity * Sale::price) - 
            ((Sale::quantity * Sale::price) * (Sale::commissionRate / 100))
        )
    }
    .toList()
```

## Computed Flags

Add boolean flags:
```kotlin
konduct.collection<Product>()
    .addFields {
        "isFeatured" from (Product::rating gte 4.5)
        "needsRestock" from (Product::stock lte Product::reorderPoint)
        "onSale" from (Product::discount gt 0)
        "isPremium" from (Product::price gte 1000)
    }
    .toList()
```

## Replacing Fields

Override existing fields:
```kotlin
konduct.collection<Product>()
    .addFields {
        // Update price with discount applied
        "price" from (Product::price * (1 - (Product::discount / 100)))
        
        // Normalize status
        "status" from when {
            Product::stock eq 0 -> "unavailable"
            Product::active eq false -> "inactive"
            otherwise -> "active"
        }
    }
    .toList()
```

## Nested Field Creation

Create nested objects:
```kotlin
.addFields {
    "address" from doc {
        "street" from User::street
        "city" from User::city
        "country" from User::country
    }
    "metadata" from doc {
        "createdAt" from Date()
        "version" from 1
    }
}
```

## Combining with Other Stages

### Match → AddFields → Sort
```kotlin
konduct.collection<Product>()
    .match { Product::status eq "active" }
    .addFields {
        "profitMargin" from (
            ((Product::sellingPrice - Product::costPrice) / Product::sellingPrice) * 100
        )
    }
    .sort { "profitMargin".desc() }
    .limit(10)
    .toList()
```

### Group → AddFields
```kotlin
konduct.collection<Sale>()
    .group {
        by(Sale::productId)
        accumulate {
            "revenue" sum (Sale::quantity * Sale::price)
            "unitsSold" sum Sale::quantity
        }
    }
    .addFields {
        "avgPricePerUnit" from ("\$revenue" / "\$unitsSold")
    }
    .toList()
```

## Using Expressions

Leverage the expression system:
```kotlin
import io.github.denofbits.konduct.expressions.*

konduct.collection<Order>()
    .addFields {
        "total" from (
            (OrderItem::quantity * OrderItem::price) +
            (OrderItem::quantity * OrderItem::price * (OrderItem::taxRate / 100)) -
            (OrderItem::quantity * OrderItem::price * (OrderItem::discountRate / 100))
        )
    }
    .toList()
```

## Performance Tips

1. **Add fields after filtering:**
```kotlin
   // ✅ Good
   .match { Product::status eq "active" }
   .addFields { /* computations */ }
   
   // ❌ Bad - computing for all documents
   .addFields { /* computations */ }
   .match { Product::status eq "active" }
```

2. **Avoid expensive computations:**
```kotlin
   // Consider pre-computing in application or database
   .addFields {
       "complexCalculation" from heavyComputation()  // May be slow
   }
```

3. **Reuse computed fields:**
```kotlin
   .addFields {
       "subtotal" from (quantity * price)
   }
   .addFields {
       "total" from ("\$subtotal" + "\$tax")  // Reuse subtotal
   }
```

## See Also

- [Expressions](expressions.md) - Arithmetic operations
- [Grouping](grouping.md) - Aggregate data
- [Match & Filter](match.md) - Filter results