# Installation

## Requirements

- Kotlin 1.9.22 or higher
- Java 17 or higher
- Spring Boot 3.0+ (for Spring Data MongoDB)
- MongoDB 4.4+ (5.0+ recommended)

## Add Dependency

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
    <dependencies>
        <dependency>
            <groupId>io.github.denofbits</groupId>
            <artifactId>konduct</artifactId>
            <version>0.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
            <version>3.2.1</version>
        </dependency>
    </dependencies>
```

## Configure MongoDB

Add MongoDB connection to your `application.yml`:
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

## Verify Installation

Create a simple test:
```kotlin
import io.github.denofbits.konduct.core.Konduct
import org.springframework.boot.CommandLineRunner
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class KonductTest(mongoTemplate: MongoTemplate) : CommandLineRunner {
    
    private val konduct = Konduct(mongoTemplate)
    
    override fun run(vararg args: String?) {
        val count = konduct.collection<MyDocument>().count()
        println("Konduct is working! Document count: $count")
    }
}
```

## Next Steps

- [Quick Start](quick-start.md) - Build your first pipeline
- [Configuration](configuration.md) - Advanced setup options