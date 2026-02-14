# Faceted Search

Run multiple aggregation pipelines in parallel with the `$facet` stage.

## What is Faceting?

Faceting allows you to execute multiple aggregations on the same dataset in a single query. Perfect for analytics dashboards and search result summaries.

## Basic Facet
```kotlin
data class ProductAnalytics(
    val topProducts: List<Product>,
    val categoryBreakdown: List<Document>,
    val priceRanges: List<Document>
)

val analytics = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .facet<ProductAnalytics> {
        "topProducts" performs {
            sort { Product::rating.desc() }
            limit(10)
        }
        
        "categoryBreakdown" performs {
            group {
                by(Product::category)
                accumulate {
                    "count" count Unit
                    "avgPrice" avg Product::price
                }
            }
        }
        
        "priceRanges" performs {
            group {
                by {
                    "range" from when {
                        Product::price lt 100 -> "budget"
                        Product::price lt 500 -> "mid"
                        otherwise -> "premium"
                    }
                }
                accumulate {
                    "count" count Unit
                }
            }
        }
    }
    .firstOrNull()
```

## Available Operations in Facets

Each facet can use:

- `match { }` - Filter documents
- `sort { }` - Sort results
- `skip(n)` - Skip documents
- `limit(n)` - Limit results
- `group { }` - Aggregate data
- `count()` - Count documents

## Real-World Examples

### E-Commerce Product Search
```kotlin
data class SearchResults(
    val products: List<Product>,
    val facets: Facets
)

data class Facets(
    val categories: List<CategoryCount>,
    val priceRanges: List<PriceRange>,
    val brands: List<BrandCount>
)

fun searchWithFacets(query: String): SearchResults? {
    return konduct.collection<Product>()
        .match {
            Product::name regex query.toRegex(RegexOption.IGNORE_CASE)
            Product::status eq "active"
        }
        .facet<SearchResults> {
            "products" performs {
                sort { Product::rating.desc() }
                limit(20)
            }
            
            "facets" performs {
                facet {
                    "categories" performs {
                        group {
                            by(Product::category)
                            accumulate { "count" count Unit }
                        }
                        sort { "count".desc() }
                    }
                    
                    "priceRanges" performs {
                        group {
                            by {
                                "range" from Product::price.bucket(
                                    0..50, 51..100, 101..500, 501..1000
                                )
                            }
                            accumulate { "count" count Unit }
                        }
                    }
                    
                    "brands" performs {
                        group {
                            by(Product::brand)
                            accumulate { "count" count Unit }
                        }
                        limit(10)
                    }
                }
            }
        }
        .firstOrNull()
}
```

### Dashboard Analytics
```kotlin
data class DashboardData(
    val recentOrders: List<Order>,
    val topCustomers: List<Document>,
    val salesByCategory: List<Document>,
    val totalRevenue: List<Document>
)

fun getDashboard(startDate: Date): DashboardData? {
    return konduct.collection<Order>()
        .match {
            Order::orderDate gte startDate
            Order::status eq "completed"
        }
        .facet<DashboardData> {
            "recentOrders" performs {
                sort { Order::orderDate.desc() }
                limit(20)
            }
            
            "topCustomers" performs {
                group {
                    by(Order::customerId)
                    accumulate {
                        "totalSpent" sum Order::total
                        "orderCount" count Unit
                    }
                }
                sort { "totalSpent".desc() }
                limit(10)
            }
            
            "salesByCategory" performs {
                group {
                    by(Order::category)
                    accumulate {
                        "revenue" sum Order::total
                        "orders" count Unit
                    }
                }
                sort { "revenue".desc() }
            }
            
            "totalRevenue" performs {
                group {
                    by { }  // No grouping, aggregate all
                    accumulate {
                        "total" sum Order::total
                        "count" count Unit
                        "average" avg Order::total
                    }
                }
            }
        }
        .firstOrNull()
}
```

### User Activity Report
```kotlin
data class ActivityReport(
    val dailyActivity: List<Document>,
    val topPages: List<Document>,
    val deviceBreakdown: List<Document>
)

fun getActivityReport(userId: String, days: Int): ActivityReport? {
    val since = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000)
    
    return konduct.collection<PageView>()
        .match {
            PageView::userId eq userId
            PageView::timestamp gte since
        }
        .facet<ActivityReport> {
            "dailyActivity" performs {
                group {
                    by(PageView::timestamp, unit = TimeUnit.DAY)
                    accumulate {
                        "views" count Unit
                        "uniquePages" countDistinct PageView::page
                    }
                }
                sort { "timestamp".asc() }
            }
            
            "topPages" performs {
                group {
                    by(PageView::page)
                    accumulate {
                        "views" count Unit
                    }
                }
                sort { "views".desc() }
                limit(10)
            }
            
            "deviceBreakdown" performs {
                group {
                    by(PageView::device)
                    accumulate {
                        "count" count Unit
                    }
                }
            }
        }
        .firstOrNull()
}
```

## Combining Facets with Filters
```kotlin
// Pre-filter, then facet
konduct.collection<Product>()
    .match {
        Product::status eq "active"
        Product::inStock eq true
    }
    .facet<Results> {
        "byCategory" performs {
            group {
                by(Product::category)
                accumulate { "count" count Unit }
            }
        }
        
        "byPrice" performs {
            group {
                by(Product::priceRange)
                accumulate { "count" count Unit }
            }
        }
    }
    .firstOrNull()
```

## Performance Tips

1. **Filter before faceting:**
```kotlin
   // ✅ Good
   .match { Product::status eq "active" }
   .facet { /* ... */ }
```

2. **Limit facet results:**
```kotlin
   "topProducts" performs {
       limit(20)  // Don't return thousands
   }
```

3. **Use indexes:**
```kotlin
   @Indexed
   data class Product(
       @Indexed val status: String,
       @Indexed val category: String
   )
```

## See Also

- [Grouping](grouping.md) - Aggregate within facets
- [Pagination](pagination.md) - Paginate facet results
- [Examples](../examples/analytics.md) - More facet examples