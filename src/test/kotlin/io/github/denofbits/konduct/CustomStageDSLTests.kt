package io.github.denofbits.konduct

import io.github.denofbits.konduct.core.Konduct
import io.github.denofbits.konduct.expressions.concat
import io.github.denofbits.konduct.expressions.times
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.util.*
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
class CustomStageDSLTests {
    
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
    fun `should perform custom stage`() {
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

        // When
        val results = konduct.collection<Product>()
            .customStage("\$redact") {
                "\$cond" from {
                    "if" from doc { "\$eq" from list("\$name", "Laptop") }
                    "then" from "\$\$PRUNE"
                    "else" from "\$\$DESCEND"
                }
            }
            .toList()

        // Then
        assertEquals(2, results.size)

    }


}
