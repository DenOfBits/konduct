# Expressions & Calculations

Use natural Kotlin operators for complex calculations in your pipelines.

## Basic Arithmetic

Use standard operators on property references:
```kotlin
konduct.collection<OrderItem>()
    .group {
        by(OrderItem::orderId)
        accumulate {
            "totalRevenue" sum (OrderItem::quantity * OrderItem::price)
            "totalCost" sum (OrderItem::quantity * OrderItem::cost)
            "totalProfit" sum ((OrderItem::price - OrderItem::cost) * OrderItem::quantity)
        }
    }
    .toList()
```

## Supported Operators

### Multiplication
```kotlin
Product::price * Product::quantity
Product::price * 1.2  // Add 20% markup
```

### Addition
```kotlin
Product::basePrice + Product::tax
```

### Subtraction
```kotlin
Product::originalPrice - Product::discount
```

### Division
```kotlin
Product::total / Product::quantity  // Average per item
Product::taxRate / 100              // Convert percentage
```

## Complex Expressions

Chain multiple operations:
```kotlin
accumulate {
    "netRevenue" sum (
        (OrderItem::quantity * OrderItem::price) - 
        (OrderItem::quantity * OrderItem::price * (OrderItem::discountRate / 100))
    )
    
    "profitMargin" avg (
        ((OrderItem::price - OrderItem::cost) / OrderItem::price) * 100
    )
}
```

## Using in AddFields

Add calculated fields to documents:
```kotlin
konduct.collection<Product>()
    .addFields {
        "totalValue" from (Product::stock * Product::price)
        "discountedPrice" from (Product::price * (1 - (Product::discount / 100)))
        "margin" from ((Product::price - Product::cost) / Product::price * 100)
    }
    .toList()
```

## Real-World Examples

### E-Commerce Revenue Calculation
```kotlin
konduct.collection<OrderItem>()
    .group {
        by(OrderItem::orderId)
        accumulate {
            // Subtotal
            "subtotal" sum (OrderItem::quantity * OrderItem::unitPrice)
            
            // Tax
            "tax" sum (
                OrderItem::quantity * OrderItem::unitPrice * (OrderItem::taxRate / 100)
            )
            
            // Discount
            "discount" sum (
                OrderItem::quantity * OrderItem::unitPrice * (OrderItem::discountRate / 100)
            )
            
            // Final total = Subtotal + Tax - Discount
            "total" sum (
                (OrderItem::quantity * OrderItem::unitPrice) +
                (OrderItem::quantity * OrderItem::unitPrice * (OrderItem::taxRate / 100)) -
                (OrderItem::quantity * OrderItem::unitPrice * (OrderItem::discountRate / 100))
            )
        }
    }
    .toList()
```

### Financial Portfolio Value
```kotlin
konduct.collection<Holding>()
    .group {
        by(Holding::portfolioId)
        accumulate {
            "currentValue" sum (Holding::shares * Holding::currentPrice)
            "costBasis" sum (Holding::shares * Holding::purchasePrice)
            "gainLoss" sum (
                (Holding::shares * Holding::currentPrice) - 
                (Holding::shares * Holding::purchasePrice)
            )
        }
    }
    .addFields {
        "returnPercentage" from ("gainLoss" / "costBasis" * 100)
    }
    .toList()
```

### Inventory Valuation
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

## Expression Types

All expressions implement the `Expression` interface and can be combined:
```kotlin
// Field reference
Product::price  // Becomes FieldExpression

// Literal value
100             // Becomes LiteralExpression

// Operations
Product::price * 2                    // MultiplyExpression
Product::price + Product::tax         // AddExpression
Product::price - Product::discount    // SubtractExpression
Product::total / Product::quantity    // DivideExpression
```

## Type Safety

Expressions maintain type safety:
```kotlin
// ✅ Correct - Number types
Product::price * Product::quantity

// ❌ Won't compile - Can't multiply non-numbers
Product::name * Product::description
```

## See Also

- [Grouping](grouping.md) - Use expressions in aggregations
- [Add Fields](../advanced/add-fields.md) - Add calculated fields
- [API Reference](../api/index.html) - Complete expression API