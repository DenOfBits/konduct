package io.github.denofbits.konduct

import io.github.denofbits.konduct.core.Konduct
import io.github.denofbits.konduct.core.PagedResult
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.query.gte
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Document("products")
data class Product(
    @Id val id: ObjectId = ObjectId.get(),
    val name: String,
    val price: Double,
    val category: String,
    val status: String,
    val rating: Double? = null,
    @Field("in_stock") val inStock: Boolean = true
)

@Configuration
@SpringBootApplication
class TestConfiguration

/*@Testcontainers
@DataMongoTest*/
@DataMongoTest
@TestPropertySource(properties = [
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=27017",
    "spring.data.mongodb.database=konduct_test"
])
@ContextConfiguration(classes = [TestConfiguration::class])
class KonductIntegrationTest {
    
    /*companion object {
        @Container
        val mongoContainer = MongoDBContainer("mongo:7.0")
        
        @JvmStatic
        @DynamicPropertySource
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl)
        }
    }*/
    
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun cleanup() {
        // Clean up before each test
        mongoTemplate.dropCollection(Product::class.java)
    }

    @Test
    fun `should filter products by status`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Laptop", price = 1200.0, category = "Electronics", status = "active"),
                Product(name = "Mouse", price = 25.0, category = "Electronics", status = "inactive"),
                Product(name = "Keyboard", price = 75.0, category = "Electronics", status = "active")
            )
        )
        
        // When
        val results = konduct.collection<Product>()
            .match { Product::status eq "active" }
            .toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.status == "active" })
    }
    
    @Test
    fun `should filter products by price range`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Budget Mouse", price = 10.0, category = "Electronics", status = "active"),
                Product(name = "Premium Mouse", price = 50.0, category = "Electronics", status = "active"),
                Product(name = "Gaming Mouse", price = 150.0, category = "Electronics", status = "active")
            )
        )
        
        // When
        val results = konduct.collection<Product>()
            .match {
                Product::price gte 20.0
                Product::price lte 100.0
            }
            .toList()
        
        // Then
        assertEquals(1, results.size)
        assertEquals("Premium Mouse", results[0].name)
    }
    
    @Test
    fun `should sort products by price descending`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Cheap", price = 10.0, category = "Electronics", status = "active"),
                Product(name = "Expensive", price = 100.0, category = "Electronics", status = "active"),
                Product(name = "Medium", price = 50.0, category = "Electronics", status = "active")
            )
        )
        
        // When
        val results = konduct.collection<Product>()
            .match { Product::status eq "active" }
            .sort { Product::price.desc() }
            .toList()
        
        // Then
        assertEquals(3, results.size)
        assertEquals("Expensive", results[0].name)
        assertEquals("Medium", results[1].name)
        assertEquals("Cheap", results[2].name)
    }
    
    @Test
    fun `should limit results`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            (1..10).map { 
                Product(name = "Product $it", price = it * 10.0, category = "Electronics", status = "active") 
            }
        )
        
        // When
        val results = konduct.collection<Product>()
            .match { Product::status eq "active" }
            .limit(3)
            .toList()
        
        // Then
        assertEquals(3, results.size)
    }
    
    @Test
    fun `should skip and limit for pagination`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            (1..10).map { 
                Product(name = "Product $it", price = it * 10.0, category = "Electronics", status = "active") 
            }
        )
        
        // When
        val results = konduct.collection<Product>()
            .match { Product::status eq "active" }
            .sort { Product::name.asc() }
            .skip(3)
            .limit(3)
            .toList()
        
        // Then
        assertEquals(3, results.size)
    }
    
    @Test
    fun `should get first result or null`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insert(Product(name = "Test Product", price = 100.0, category = "Electronics", status = "active"))
        
        // When - result exists
        val result = konduct.collection<Product>()
            .match { Product::name eq "Test Product" }
            .firstOrNull()
        
        // Then
        assertNotNull(result)
        assertEquals("Test Product", result.name)
        
        // When - no result
        val noResult = konduct.collection<Product>()
            .match { Product::name eq "Non-existent" }
            .firstOrNull()
        
        // Then
        assertEquals(null, noResult)
    }
    
    @Test
    fun `should count matching documents`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Active 1", price = 100.0, category = "Electronics", status = "active"),
                Product(name = "Active 2", price = 200.0, category = "Electronics", status = "active"),
                Product(name = "Inactive", price = 300.0, category = "Electronics", status = "inactive")
            )
        )
        
        // When
        val count = konduct.collection<Product>()
            .match { Product::status eq "active" }
            .count()
        
        // Then
        assertEquals(2, count)
    }
    
    @Test
    fun `should filter with in operator`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Electronics", price = 100.0, category = "Electronics", status = "active"),
                Product(name = "Book", price = 20.0, category = "Books", status = "active"),
                Product(name = "Clothing", price = 50.0, category = "Clothing", status = "active")
            )
        )
        
        // When
        val results = konduct.collection<Product>()
            .match {
                Product::category `in` listOf("Electronics", "Books")
            }
            .toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results.any { it.category == "Electronics" })
        assertTrue(results.any { it.category == "Books" })
    }
    
    @Test
    fun `should combine multiple conditions`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8),
                Product(name = "Budget Laptop", price = 500.0, category = "Electronics", status = "active", rating = 3.5),
                Product(name = "Premium Phone", price = 1200.0, category = "Electronics", status = "inactive", rating = 4.9)
            )
        )
        
        // When
        val results = konduct.collection<Product>()
            .match {
                Product::category eq "Electronics"
                Product::status eq "active"
                Product::price gte 1000.0
                Product::rating gte 4.0
            }
            .toList()
        
        // Then
        assertEquals(1, results.size)
        assertEquals("Premium Laptop", results[0].name)
    }

    @Test
    fun `should perform filters on id field`() {


        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(id = ObjectId("6977724554db11048724b730"), name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8),
                Product(id = ObjectId("6977724554db11048724b731"), name = "Budget Laptop", price = 500.0, category = "Electronics", status = "active", rating = 3.5),
                Product(id = ObjectId("6977724554db11048724b732"), name = "Premium Phone", price = 1200.0, category = "Electronics", status = "inactive", rating = 4.9)
            )
        )

        // When
        val results = konduct.collection<Product>()
            .match { Product::id eq ObjectId("6977724554db11048724b730") }
            .toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results.all { it.id == ObjectId("6977724554db11048724b730") })
    }

    @Test
    fun `should perform filters on custom named fields`() {

        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8, inStock = true),
                Product(name = "Budget Laptop", price = 500.0, category = "Electronics", status = "active", rating = 3.5, inStock = true),
                Product(name = "Premium Phone", price = 1200.0, category = "Electronics", status = "inactive", rating = 4.9, inStock = false)

            )
        )

        // When
        val results = konduct.collection<Product>()
            .match { Product::inStock eq true }
            .toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.inStock })
    }

    @Test
    fun `should paginate`() {

        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8, inStock = true),
                Product(name = "Budget Laptop", price = 500.0, category = "Electronics", status = "active", rating = 3.5, inStock = true),
                Product(name = "Headset", price = 1500.0, category = "Electronics", status = "active", rating = 4.8, inStock = true),
                Product(name = "Smartphone", price = 500.0, category = "Electronics", status = "active", rating = 3.5, inStock = true),
                Product(name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8, inStock = true),
                Product(name = "Charger", price = 500.0, category = "Electronics", status = "active", rating = 3.5, inStock = true),
                Product(name = "Premium Laptop", price = 1500.0, category = "Electronics", status = "active", rating = 4.8, inStock = true),
                Product(name = "DELL Laptop", price = 500.0, category = "Electronics", status = "active", rating = 3.5, inStock = true),
                Product(name = "Premium Speaker", price = 1200.0, category = "Electronics", status = "inactive", rating = 4.9, inStock = false)

            )
        )

        // When
        val results : PagedResult<Product>? = konduct.collection<Product>()
            .match { Product::inStock eq true }
            .paginate(page = 1, pageSize = 3)
            .firstOrNull()

        // Then
        assertEquals(8, results?.total)
        assertEquals(1, results?.page)
        assertEquals(3, results?.pageSize)
        assertEquals(3, results?.data?.size)
        assertTrue(results!!.data.all { it.inStock })

    }
}
