# Konduct

> A Kotlin-first DSL for MongoDB aggregation pipelines

**Konduct** provides a type-safe, fluent API for building MongoDB aggregation pipelines in Kotlin. Write aggregations that feel natural and catch errors at compile time.

## Features

- 🎯 **Type-Safe** - Compile-time field validation using Kotlin property references
- 🔗 **Fluent API** - Chain operations naturally like `collection<T>().match{}.sort{}.limit()`
- 📝 **MongoDB-Aligned** - Syntax mirrors MongoDB operations for familiarity
- 🚀 **Spring Integration** - Seamless integration with Spring Data MongoDB
- ✨ **Expression System** - Natural Kotlin operators for calculations
- 📊 **Built-in Patterns** - Pagination, facets, grouping, and more

## Quick Example
```kotlin
val results = konduct.collection<Product>()
    .match { 
        Product::status eq "active"
        Product::price gte 100
    }
    .group {
        by(Product::category)
        accumulate {
            "totalRevenue" sum (Product::quantity * Product::price)
            "avgPrice" avg Product::price
            "count" count Unit
        }
    }
    .sort { "totalRevenue".desc() }
    .toList()
```

## Installation

=== "Gradle (Kotlin DSL)"
```kotlin
    dependencies {
        implementation("io.github.denofbits:konduct:0.1.0")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.2.1")
    }
```

=== "Gradle (Groovy)"
```groovy
    dependencies {
        implementation 'io.github.denofbits:konduct:0.1.0'
        implementation 'org.springframework.boot:spring-boot-starter-data-mongodb:3.2.1'
    }
```

=== "Maven"
```xml
    <dependency>
        <groupId>io.github.denofbits</groupId>
        <artifactId>konduct</artifactId>
        <version>0.1.0</version>
    </dependency>
```

## Next Steps

- [Quick Start Guide](getting-started/quick-start.md) - Get up and running in 5 minutes
- [Core Concepts](guide/core-concepts.md) - Understand the fundamentals
- [Examples](examples/ecommerce.md) - Real-world usage patterns
- [API Reference](api/index.html) - Complete API documentation