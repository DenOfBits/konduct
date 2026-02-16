# Analytics Examples

Advanced analytics and reporting patterns.

## Time-Series Analysis

### Monthly Revenue Trends
```kotlin
data class MonthlyRevenue(
    val year: Int,
    val month: Int,
    val revenue: Double,
    val orders: Int,
    val avgOrderValue: Double,
    val revenue_previous: Double?,
    val revenue_deviation: Double?,
    val revenue_deviationPct: Double?
)

fun getMonthlyRevenueTrends(year: Int): List<MonthlyRevenue> {
    val startDate = Date(year, 0, 1)
    val endDate = Date(year, 11, 31)
    
    return konduct.collection<Order>()
        .match {
            Order::orderDate gte startDate
            Order::orderDate lte endDate
            Order::status eq "completed"
        }
        .group {
            by(Order::orderDate, unit = TimeUnit.MONTH)
            accumulate {
                "revenue" sum Order::total
                "orders" count Unit
                "avgOrderValue" avg Order::total
            }
            trackPrevious {
                fields("revenue", "orders")
                calculateDeviation = true
                calculatePercentage = true
            }
        }
        .sort { "period".asc() }
        .into<MonthlyRevenue>()
        .toList()
}
```

### Week-over-Week Growth
```kotlin
data class WeeklyGrowth(
    val week: String,
    val sales: Double,
    val previousWeek: Double?,
    val growth: Double?,
    val growthPct: Double?
)

fun getWeeklyGrowth(weeks: Int = 12): List<WeeklyGrowth> {
    return konduct.collection<Sale>()
        .match {
            Sale::date gte weeksAgo(weeks)
        }
        .group {
            by(Sale::date, unit = TimeUnit.WEEK)
            accumulate {
                "sales" sum Sale::amount
            }
            trackPrevious {
                fields("sales")
                calculateDeviation = true
                calculatePercentage = true
            }
        }
        .sort { "week".asc() }
        .into<WeeklyGrowth>()
        .toList()
}
```

## Cohort Analysis

### User Retention by Signup Month
```kotlin
data class CohortRetention(
    val cohortMonth: String,
    val totalUsers: Int,
    val activeMonth1: Int,
    val activeMonth2: Int,
    val activeMonth3: Int,
    val retentionMonth1Pct: Double,
    val retentionMonth2Pct: Double,
    val retentionMonth3Pct: Double
)

fun getUserRetentionCohorts(): List<CohortRetention> {
    return konduct.collection<User>()
        .group {
            by {
                "cohortMonth" from User::signupDate.yearMonth()
            }
            accumulate {
                "totalUsers" count Unit
                "userIds" push User::id
            }
        }
        .lookup {
            from<Activity>()
            let {
                "userList" to "\$userIds"
                "cohortDate" to "\$cohortMonth"
            }
            pipeline {
                match {
                    expr {
                        and(
                            `in`("\$userId", "$$userList"),
                            gte("\$activityDate", "$$cohortDate")
                        )
                    }
                }
                group {
                    by {
                        "userId" from "\$userId"
                        "monthsAfter" from monthsDiff("$$cohortDate", "\$activityDate")
                    }
                }
                group {
                    by("\$_id.monthsAfter")
                    accumulate {
                        "activeUsers" countDistinct "\$_id.userId"
                    }
                }
            }
            into("retention")
        }
        .addFields {
            "activeMonth1" from arrayElemAt("\$retention", 1, "activeUsers")
            "activeMonth2" from arrayElemAt("\$retention", 2, "activeUsers")
            "activeMonth3" from arrayElemAt("\$retention", 3, "activeUsers")
            "retentionMonth1Pct" from ("activeMonth1" / "totalUsers" * 100)
            "retentionMonth2Pct" from ("activeMonth2" / "totalUsers" * 100)
            "retentionMonth3Pct" from ("activeMonth3" / "totalUsers" * 100)
        }
        .into<CohortRetention>()
        .toList()
}
```

## Funnel Analysis

### Conversion Funnel
```kotlin
data class ConversionFunnel(
    val stage: String,
    val users: Int,
    val dropoff: Int?,
    val conversionRate: Double?
)

fun getConversionFunnel(startDate: Date, endDate: Date): List<ConversionFunnel> {
    return konduct.collection<Event>()
        .match {
            Event::timestamp gte startDate
            Event::timestamp lte endDate
        }
        .facet<FunnelData> {
            "visited" performs {
                match { Event::type eq "page_view" }
                group {
                    by { }
                    accumulate {
                        "users" countDistinct Event::userId
                    }
                }
            }
            
            "addedToCart" performs {
                match { Event::type eq "add_to_cart" }
                group {
                    by { }
                    accumulate {
                        "users" countDistinct Event::userId
                    }
                }
            }
            
            "checkout" performs {
                match { Event::type eq "checkout_started" }
                group {
                    by { }
                    accumulate {
                        "users" countDistinct Event::userId
                    }
                }
            }
            
            "purchased" performs {
                match { Event::type eq "purchase_completed" }
                group {
                    by { }
                    accumulate {
                        "users" countDistinct Event::userId
                    }
                }
            }
        }
        .firstOrNull()
        ?.let { calculateFunnelMetrics(it) }
        ?: emptyList()
}
```

## Product Analytics

### Feature Usage Statistics
```kotlin
data class FeatureUsage(
    val feature: String,
    val totalUses: Int,
    val uniqueUsers: Int,
    val avgUsesPerUser: Double,
    val lastUsed: Date
)

fun getFeatureUsage(days: Int = 30): List<FeatureUsage> {
    return konduct.collection<FeatureEvent>()
        .match {
            FeatureEvent::timestamp gte daysAgo(days)
        }
        .group {
            by(FeatureEvent::featureName)
            accumulate {
                "totalUses" count Unit
                "uniqueUsers" countDistinct FeatureEvent::userId
                "lastUsed" max FeatureEvent::timestamp
            }
        }
        .addFields {
            "avgUsesPerUser" from ("totalUses" / "uniqueUsers")
        }
        .sort { "totalUses".desc() }
        .into<FeatureUsage>()
        .toList()
}
```

### User Engagement Score
```kotlin
data class UserEngagement(
    val userId: String,
    val loginCount: Int,
    val featureUsage: Int,
    val contentCreated: Int,
    val engagementScore: Double,
    val tier: String
)

fun calculateEngagementScores(): List<UserEngagement> {
    return konduct.collection<UserActivity>()
        .match {
            UserActivity::timestamp gte daysAgo(30)
        }
        .group {
            by(UserActivity::userId)
            accumulate {
                "loginCount" sumIf { UserActivity::type eq "login" }
                "featureUsage" sumIf { UserActivity::type eq "feature_use" }
                "contentCreated" sumIf { UserActivity::type eq "content_created" }
            }
        }
        .addFields {
            "engagementScore" from (
                ("loginCount" * 1) +
                ("featureUsage" * 2) +
                ("contentCreated" * 5)
            )
            "tier" from when {
                "engagementScore" gte 100 -> "power_user"
                "engagementScore" gte 50 -> "active"
                "engagementScore" gte 10 -> "casual"
                otherwise -> "inactive"
            }
        }
        .sort { "engagementScore".desc() }
        .into<UserEngagement>()
        .toList()
}
```

## Real-Time Dashboard

### Dashboard Data (Multiple Metrics)
```kotlin
data class DashboardMetrics(
    val todayRevenue: Double,
    val todayOrders: Int,
    val activeUsers: Int,
    val topProducts: List<Product>,
    val recentOrders: List<Order>,
    val categoryBreakdown: List<CategoryStats>
)

fun getDashboardMetrics(): DashboardMetrics? {
    val today = startOfDay(Date())
    
    return konduct.collection<Order>()
        .facet<DashboardMetrics> {
            "todayStats" performs {
                match {
                    Order::orderDate gte today
                    Order::status eq "completed"
                }
                group {
                    by { }
                    accumulate {
                        "todayRevenue" sum Order::total
                        "todayOrders" count Unit
                    }
                }
            }
            
            "activeUsers" performs {
                match {
                    Order::orderDate gte daysAgo(30)
                }
                group {
                    by { }
                    accumulate {
                        "activeUsers" countDistinct Order::customerId
                    }
                }
            }
            
            "topProducts" performs {
                match {
                    Order::orderDate gte daysAgo(7)
                    Order::status eq "completed"
                }
                group {
                    by(Order::productId)
                    accumulate {
                        "revenue" sum Order::total
                    }
                }
                sort { "revenue".desc() }
                limit(5)
                lookup {
                    from<Product>()
                    localField("_id")
                    foreignField(Product::id)
                    into("productInfo")
                }
            }
            
            "recentOrders" performs {
                sort { Order::orderDate.desc() }
                limit(10)
            }
            
            "categoryBreakdown" performs {
                match { Order::status eq "completed" }
                lookup {
                    from<Product>()
                    localField(Order::productId)
                    foreignField(Product::id)
                    into("product")
                }
                unwind("product")
                group {
                    by("\$product.category")
                    accumulate {
                        "revenue" sum Order::total
                        "orders" count Unit
                    }
                }
                sort { "revenue".desc() }
            }
        }
        .firstOrNull()
}
```

## See Also

- [E-Commerce Examples](ecommerce.md) - Sales and inventory
- [Grouping Guide](../guide/grouping.md) - Aggregation basics
- [Facets Guide](../guide/facets.md) - Multi-metric queries