package io.github.denofbits.konduct

import io.github.denofbits.konduct.core.Konduct
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals


/*@Testcontainers
@DataMongoTest*/
@DataMongoTest
@TestPropertySource(properties = [
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=27017",
    "spring.data.mongodb.database=konduct_test"
])
@ContextConfiguration(classes = [TestConfiguration::class])
class GroupDSLTests {
    
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
                Product(name = "Laptop", price = 200.0, category = "Electronics", status = "active"),
                Product(name = "Shea", price = 25.0, category = "Edible", status = "inactive"),
                Product(name = "Cocoa", price = 75.0, category = "Edible", status = "active")
            )
        )

        class Summary(val category: String, val price: Double)
        // When
        val results = konduct.collection<Product>()
            .group {
                by(Product::category)
                accumulate {
                    "price" sum (Product::price)
                }
            }
            .into(Summary::class)
            .toList()

        // Then
        assertEquals(2, results.size)
        assertEquals(1400.0, results.firstOrNull { it.category == "Electronics" }?.price)
    }

}
