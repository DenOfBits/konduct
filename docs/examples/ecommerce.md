# E-Commerce Examples

Real-world patterns for e-commerce applications.

## Product Catalog

### Search Products with Filters
```kotlin
@Service
class ProductService(mongoTemplate: MongoTemplate) {
    private val konduct = Konduct(mongoTemplate)
    
    fun searchProducts(
        query: String?,
        category: String?,
        minPrice: Double?,
        maxPrice: Double?,
        inStockOnly: Boolean,
        page: Int,
        pageSize: Int
    ): PagedResult<Product> {
        return konduct.collection<Product>()
            .match {
                query?.let {
                    Product::name regex it.toRegex(RegexOption.IGNORE_CASE)
                }
                category?.let { Product::category eq it }
                minPrice?.let { Product::price gte it }
                maxPrice?.let { Product::price lte it }
                if (inStockOnly) {
                    Product::inStock eq true
                }
                Product::status eq "active"
            }
            .sort {
                Product::rating.desc()
                Product::reviewCount.desc()
            }
            .paginate(page, pageSize)
            .firstOrNull() ?: PagedResult(
                data = emptyList(),
                total = 0,
                page = page,
                pageSize = pageSize,
                totalPages = 0
            )
    }
}
```

### Related Products
```kotlin
fun getRelatedProducts(productId: String, limit: Int = 5): List<Product> {
    val product = konduct.collection<Product>()
        .match { Product::id eq productId }
        .firstOrNull() ?: return emptyList()
    
    return konduct.collection<Product>()
        .match {
            Product::category eq product.category
            Product::id ne productId
            Product::status eq "active"
            Product::inStock eq true
        }
        .sort { Product::rating.desc() }
        .limit(limit)
        .toList()
}
```

## Order Management

### Customer Order History
```kotlin
data class OrderSummary(
    val _id: String,
    val orderDate: Date,
    val total: Double,
    val itemCount: Int,
    val status: String
)

fun getCustomerOrders(customerId: String, limit: Int = 20): List<OrderSummary> {
    return konduct.collection<Order>()
        .match { Order::customerId eq customerId }
        .sort { Order::orderDate.desc() }
        .limit(limit)
        .into<OrderSummary>()
        .toList()
}
```

### Order Totals Calculation
```kotlin
data class OrderTotal(
    val orderId: String,
    val subtotal: Double,
    val tax: Double,
    val discount: Double,
    val shipping: Double,
    val total: Double
)

fun calculateOrderTotals(orderId: String): OrderTotal? {
    return konduct.collection<OrderItem>()
        .match { OrderItem::orderId eq orderId }
        .group {
            by(OrderItem::orderId)
            accumulate {
                "subtotal" sum (OrderItem::quantity * OrderItem::price)
                "tax" sum (
                    OrderItem::quantity * OrderItem::price * (OrderItem::taxRate / 100)
                )
                "discount" sum (
                    OrderItem::quantity * OrderItem::price * (OrderItem::discountRate / 100)
                )
            }
        }
        .addFields {
            "shipping" from 10.0  // Flat rate
            "total" from (
                "subtotal" + "tax" - "discount" + "shipping"
            )
        }
        .into<OrderTotal>()
        .firstOrNull()
}
```

## Customer Analytics

### Customer Lifetime Value
```kotlin
data class CustomerLTV(
    val customerId: String,
    val totalSpent: Double,
    val orderCount: Int,
    val avgOrderValue: Double,
    val firstOrderDate: Date,
    val lastOrderDate: Date,
    val daysSinceFirst: Long,
    val daysSinceLast: Long
)

fun getHighValueCustomers(minSpent: Double = 1000): List<CustomerLTV> {
    return konduct.collection<Order>()
        .match {
            Order::status `in` listOf("completed", "shipped")
        }
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
        .addFields {
            "daysSinceFirst" from dateDiffDays("firstOrderDate", Date())
            "daysSinceLast" from dateDiffDays("lastOrderDate", Date())
        }
        .match { "totalSpent" gte minSpent }
        .sort { "totalSpent".desc() }
        .into<CustomerLTV>()
        .toList()
}
```

### Purchase Frequency
```kotlin
data class PurchaseFrequency(
    val customerId: String,
    val totalOrders: Int,
    val daysBetweenOrders: Double,
    val isFrequentBuyer: Boolean
)

fun analyzePurchaseFrequency(): List<PurchaseFrequency> {
    return konduct.collection<Order>()
        .match { Order::status eq "completed" }
        .group {
            by(Order::customerId)
            accumulate {
                "totalOrders" count Unit
                "firstOrder" min Order::orderDate
                "lastOrder" max Order::orderDate
            }
        }
        .addFields {
            "daysBetweenOrders" from (
                dateDiffDays("firstOrder", "lastOrder") / ("totalOrders" - 1)
            )
            "isFrequentBuyer" from ("daysBetweenOrders" lt 30)
        }
        .match { "totalOrders" gte 2 }
        .into<PurchaseFrequency>()
        .toList()
}
```

## Inventory Management

### Low Stock Alert
```kotlin
data class LowStockProduct(
    val id: String,
    val name: String,
    val category: String,
    val currentStock: Int,
    val reorderPoint: Int,
    val daysOfStock: Int
)

fun getLowStockProducts(): List<LowStockProduct> {
    return konduct.collection<Product>()
        .match {
            Product::status eq "active"
        }
        .addFields {
            "daysOfStock" from (
                Product::stock / Product::avgDailySales
            )
        }
        .match {
            or(
                "stock" lte "reorderPoint",
                "daysOfStock" lte 7
            )
        }
        .sort { "daysOfStock".asc() }
        .into<LowStockProduct>()
        .toList()
}
```

### Inventory Valuation
```kotlin
data class InventoryValue(
    val category: String,
    val productCount: Int,
    val totalUnits: Int,
    val costValue: Double,
    val retailValue: Double,
    val potentialProfit: Double
)

fun getInventoryValuation(): List<InventoryValue> {
    return konduct.collection<Product>()
        .match { Product::status eq "active" }
        .group {
            by(Product::category)
            accumulate {
                "productCount" count Unit
                "totalUnits" sum Product::stock
                "costValue" sum (Product::stock * Product::costPrice)
                "retailValue" sum (Product::stock * Product::sellingPrice)
            }
        }
        .addFields {
            "potentialProfit" from ("retailValue" - "costValue")
        }
        .sort { "retailValue".desc() }
        .into<InventoryValue>()
        .toList()
}
```

## Sales Analytics

### Daily Sales Report
```kotlin
data class DailySales(
    val date: String,
    val revenue: Double,
    val orders: Int,
    val avgOrderValue: Double,
    val uniqueCustomers: Int
)

fun getDailySales(startDate: Date, endDate: Date): List<DailySales> {
    return konduct.collection<Order>()
        .match {
            Order::status eq "completed"
            Order::orderDate gte startDate
            Order::orderDate lte endDate
        }
        .group {
            by(Order::orderDate, unit = TimeUnit.DAY)
            accumulate {
                "revenue" sum Order::total
                "orders" count Unit
                "avgOrderValue" avg Order::total
                "uniqueCustomers" countDistinct Order::customerId
            }
        }
        .sort { "date".asc() }
        .into<DailySales>()
        .toList()
}
```

### Top Selling Products
```kotlin
data class ProductSales(
    val productId: String,
    val productName: String,
    val unitsSold: Int,
    val revenue: Double,
    val orderCount: Int
)

fun getTopSellingProducts(days: Int = 30, limit: Int = 10): List<ProductSales> {
    val since = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000)
    
    return konduct.collection<OrderItem>()
        .match {
            OrderItem::orderDate gte since
            OrderItem::status eq "completed"
        }
        .group {
            by(OrderItem::productId)
            accumulate {
                "unitsSold" sum OrderItem::quantity
                "revenue" sum (OrderItem::quantity * OrderItem::price)
                "orderCount" count Unit
            }
        }
        .lookup {
            from<Product>()
            localField("_id")
            foreignField(Product::id)
            into("product")
        }
        .unwind("product")
        .addFields {
            "productName" from "\$product.name"
        }
        .sort { "revenue".desc() }
        .limit(limit)
        .into<ProductSales>()
        .toList()
}
```

## Abandoned Carts

### Find Abandoned Carts
```kotlin
data class AbandonedCart(
    val cartId: String,
    val customerId: String,
    val customerEmail: String,
    val itemCount: Int,
    val totalValue: Double,
    val lastUpdated: Date,
    val hoursSinceUpdate: Long
)

fun getAbandonedCarts(minHours: Int = 24): List<AbandonedCart> {
    val cutoff = Date(System.currentTimeMillis() - minHours * 60 * 60 * 1000)
    
    return konduct.collection<Cart>()
        .match {
            Cart::status eq "active"
            Cart::lastUpdated lte cutoff
            Cart::itemCount gt 0
        }
        .addFields {
            "hoursSinceUpdate" from hoursDiff(Cart::lastUpdated, Date())
        }
        .lookup {
            from<Customer>()
            localField(Cart::customerId)
            foreignField(Customer::id)
            into("customer")
        }
        .unwind("customer")
        .addFields {
            "customerEmail" from "\$customer.email"
        }
        .sort { "totalValue".desc() }
        .into<AbandonedCart>()
        .toList()
}
```

## See Also

- [Analytics Examples](analytics.md) - More analytics patterns
- [Grouping Guide](../guide/grouping.md) - Aggregation details
- [Expressions Guide](../guide/expressions.md) - Calculation patterns