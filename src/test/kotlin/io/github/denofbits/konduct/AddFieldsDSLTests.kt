package io.github.denofbits.konduct

import io.github.denofbits.konduct.core.Konduct
import io.github.denofbits.konduct.expressions.times
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
class AddFieldsDSLTests {
    
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
    fun `should add fields to document`() {
        // Given
        val konduct = Konduct(mongoTemplate)
        mongoTemplate.insertAll(
            listOf(
                Product(name = "Laptop", price = 1200.0, category = "Electronics", status = "active", ratingHistory = listOf(1, 1)),
                Product(name = "Laptop", price = 200.0, category = "Electronics", status = "active", ratingHistory = listOf(4, 1, 5)),
                Product(name = "Shea", price = 25.0, category = "Edible", status = "inactive", ratingHistory = listOf(4, 1)),
                Product(name = "Cocoa", price = 1400.0, category = "Edible", status = "active", ratingHistory = listOf(1, 2, 3))
            )
        )

        class Summary(
            val name: String,
            val price: Double,
            val category: String,
            val status: String,
            val vat: Double,
            val fullName: String,
            val avgRating: Double,
        )
        // When
        val results = konduct.collection<Product>()
            .addFields {
                "fullName" concat listOf(Product::name, " ", Product::category)
                "vat" from (Product::price * 0.18)
                "avgRating" avgOf Product::ratingHistory
            }
            .into(Summary::class)
            .toList()

        // Then
        assertEquals(4, results.size)
        assertEquals("Shea Edible", results.firstOrNull { it.name == "Shea" }?.fullName)
        assertEquals(252.0, results.firstOrNull { it.name == "Cocoa" }?.vat)
        assertEquals(2.0, results.firstOrNull { it.name == "Cocoa" }?.avgRating)

    }


}
