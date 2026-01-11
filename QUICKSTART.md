# Quick Start Guide

Get up and running with Konduct in 5 minutes!

## Prerequisites

- Java 17 or higher
- Gradle or Maven
- Spring Boot 3.0+
- MongoDB instance (local or Docker)

## Step 1: Add Dependency

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.denofbits:konduct:0.1.0-SNAPSHOT")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.2.1")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.denofbits</groupId>
    <artifactId>konduct</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Step 2: Configure MongoDB

Add to your `application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
```

Or `application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/mydb
```

## Step 3: Create Your Domain Model

```kotlin
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("products")
data class Product(
    @Id val id: String? = null,
    val name: String,
    val price: Double,
    val category: String,
    val status: String
)
```

## Step 4: Use Konduct in Your Service

```kotlin
import io.github.denofbits.konduct.core.Konduct
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class ProductService(mongoTemplate: MongoTemplate) {
    
    private val konduct = Konduct(mongoTemplate)
    
    fun getActiveProducts(): List<Product> {
        return konduct.collection<Product>()
            .match { Product::status eq "active" }
            .sort { Product::price asc }
            .toList()
    }
    
    fun getExpensiveProducts(): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::status eq "active"
                Product::price gte 100.0
            }
            .toList()
    }
    
    fun searchProducts(category: String, page: Int = 0, size: Int = 20): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::category eq category
                Product::status eq "active"
            }
            .sort { Product::name asc }
            .skip(page * size)
            .limit(size)
            .toList()
    }
}
```

## Step 5: Test It!

Create a simple controller:

```kotlin
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {
    
    @GetMapping
    fun getProducts(): List<Product> {
        return productService.getActiveProducts()
    }
    
    @GetMapping("/expensive")
    fun getExpensive(): List<Product> {
        return productService.getExpensiveProducts()
    }
    
    @GetMapping("/search")
    fun search(
        @RequestParam category: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<Product> {
        return productService.searchProducts(category, page, size)
    }
}
```

## Run Your Application

Start MongoDB (if using Docker):
```bash
docker run -d -p 27017:27017 --name mongodb mongo:7.0
```

Run your Spring Boot application:
```bash
./gradlew bootRun
```

Test the endpoints:
```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/expensive
curl "http://localhost:8080/api/products/search?category=Electronics"
```

## What's Next?

Check out the [full documentation](README.md) for:
- More complex queries
- Grouping and aggregation (coming soon)
- Joins with lookup (coming soon)
- Expression system (coming soon)

## Common Patterns

### Filtering
```kotlin
konduct.collection<Product>()
    .match {
        Product::price gte 100
        Product::category `in` listOf("Electronics", "Books")
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
konduct.collection<Product>()
    .skip(page * pageSize)
    .limit(pageSize)
    .toList()
```

### Count
```kotlin
val count = konduct.collection<Product>()
    .match { Product::status eq "active" }
    .count()
```

### Find One
```kotlin
val product = konduct.collection<Product>()
    .match { Product::id eq productId }
    .firstOrNull()
```

## Need Help?

- 📚 [Full Documentation](README.md)
- 🐛 [Report Issues](https://github.com/DenOfBits/konduct/issues)
- 💬 [Discussions](https://github.com/DenOfBits/konduct/discussions)
