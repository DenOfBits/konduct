# Konduct

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)

> A Kotlin DSL for MongoDB aggregation pipelines - Inspired by JetBrains Exposed

**Konduct** provides a type-safe, fluent API for building MongoDB aggregation pipelines in Kotlin. Write aggregations that feel natural and catch errors at compile time.

## Features

- 🎯 **Type-Safe** - Compile-time field validation using Kotlin property references
- 🔗 **Fluent API** - Chain operations naturally like `collection<T>().match{}.sort{}.limit()`
- 📝 **MongoDB-Aligned** - Syntax mirrors MongoDB operations for familiarity
- 🚀 **Spring Integration** - Seamless integration with Spring Data MongoDB
- ✨ **IDE Support** - Full autocomplete and inline documentation

## Quick Start

### Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.denofbits:konduct:1.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.2.1")
}
```

### Basic Usage

```kotlin
import io.github.denofbits.konduct.core.Konduct

@Service
class ProductService(mongoTemplate: MongoTemplate) {
    private val konduct = Konduct(mongoTemplate)
    
    fun getActiveProducts(): List<Product> {
        return konduct.collection<Product>()
            .match { 
                Product::status eq "active"
                Product::price gte 100
            }
            .sort { 
                Product::rating desc
            }
            .limit(10)
            .toList()
    }
}
```

## Comparison with Raw MongoDB

### Before (Spring Data MongoDB)

```kotlin
val matchStage = Aggregation.match(
    Criteria.where("status").`is`("active")
        .and("price").gte(100)
)
val sortStage = Aggregation.sort(Sort.Direction.DESC, "rating")
val limitStage = Aggregation.limit(10)

val aggregation = Aggregation.newAggregation(matchStage, sortStage, limitStage)
val results = mongoTemplate.aggregate(aggregation, "products", Product::class.java)
    .mappedResults
```

### After (Konduct)

```kotlin
val results = konduct.collection<Product>()
    .match { 
        Product::status eq "active"
        Product::price gte 100 
    }
    .sort { Product::rating desc }
    .limit(10)
    .toList()
```

### Extension Function Style

```kotlin
import io.github.denofbits.konduct.core.konduct

fun getProducts(mongoTemplate: MongoTemplate): List<Product> {
    return mongoTemplate.konduct()
        .collection<Product>()
        .match { Product::inStock eq true }
        .toList()
}
```

## Examples

### Filtering

```kotlin
konduct.collection<Order>()
    .match {
        Order::status eq "completed"
        Order::total gte 1000
        Order::customerId `in` listOf("c1", "c2", "c3")
    }
    .toList()
```

### Sorting

```kotlin
konduct.collection<Product>()
    .sort {
        Product::category asc
        Product::price desc
    }
    .toList()
```

### Pagination

```kotlin
konduct.collection<Article>()
    .match { Article::published eq true }
    .sort { Article::createdAt desc }
    .skip(page * pageSize)
    .limit(pageSize)
    .toList()
```

### Counting

```kotlin
val activeUserCount = konduct.collection<User>()
    .match { User::status eq "active" }
    .count()
```

### Get First Result

```kotlin
val product = konduct.collection<Product>()
    .match { Product::id eq productId }
    .firstOrNull()
```


## Requirements

- Kotlin 1.9.22 or higher
- Java 17 or higher
- Spring Boot 3.0+ (for Spring Data MongoDB)
- MongoDB 4.4+ (5.0+ recommended)

## Building from Source

```bash
git clone https://github.com/DenOfBits/konduct.git
cd konduct
./gradlew build
```

## Running Tests

```bash
./gradlew test
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Inspiration

Konduct is inspired by [JetBrains Exposed](https://github.com/JetBrains/Exposed), bringing the same philosophy of type-safe DSLs to MongoDB aggregation pipelines.

## Links

- [Documentation](https://denofbits.github.io/konduct)
- [Issue Tracker](https://github.com/DenOfBits/konduct/issues)
- [Changelog](CHANGELOG.md)

---

**Made with ❤️ by [DenOfBits](https://github.com/DenOfBits)**
