package io.github.denofbits.konduct

import io.github.denofbits.konduct.core.Konduct
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Document("customers")
data class Customer(
    @Id val id: String? = null,
    val name: String,
    val email: String,
    val tier: String
)

@Document("orderitems")
data class OrderItem(
    @Id val id: String? = null,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val price: Double
)

@Document("orders")
data class Order(
    @Id val id: String? = null,
    val customerId: String,
    val total: Double,
    val status: String,
    val orderDate: java.util.Date
)

data class OrderWithCustomers(
    val id: String,
    val total: Double,
    val customersInfos: List<Customer>
)

data class OrderWithCustomer(
    val id: String,
    val total: Double,
    val customer: Customer?
)

data class OrderWithMergedCustomer(
    @Id val id: String? = null,
    val customerId: String,
    val total: Double,
    val status: String,
    val orderDate: java.util.Date,

    val name: String,
    val email: String,
    val tier: String

)

data class OrderWithItems(
    val id: String,
    val total: Double,
    val items: List<OrderItem>
)

/*@Testcontainers
@DataMongoTest*/
@DataMongoTest
@TestPropertySource(properties = [
    "spring.data.mongodb.host=localhost",
    "spring.data.mongodb.port=27017",
    "spring.data.mongodb.database=konduct_test"
])
@ContextConfiguration(classes = [TestConfiguration::class])
class LookupDSLTests {


    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun cleanup() {
        // Clean up before each test
        mongoTemplate.dropCollection(Customer::class.java)
        mongoTemplate.dropCollection(OrderItem::class.java)
        mongoTemplate.dropCollection(Order::class.java)
    }

    @Test
    fun `should perform simple lookup`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        val customer = Customer(id = "c1", name = "John Doe", email = "john@example.com", tier = "gold")
        mongoTemplate.insert(customer)

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 100.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookup<Customer> {
                from(Customer::class)
                localField(Order::customerId)
                foreignField(Customer::id)
                into("customersInfos")
            }
            .into(OrderWithCustomers::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.firstOrNull()
        assertNotNull(doc)
        val customerInfo = doc.customersInfos
        assertNotNull(customerInfo)
        assertEquals(1, customerInfo.size)
    }

    @Test
    fun `should perform lookup with unwind`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        val customer = Customer(id = "c1", name = "Jane Smith", email = "jane@example.com", tier = "silver")
        mongoTemplate.insert(customer)

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 200.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookup<Customer> {
                from(Customer::class)
                localField(Order::customerId)
                foreignField(Customer::id)
                into("customer")
            }
            .unwind("customer", true)
            .into(OrderWithCustomer::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        val foundCostumer = doc.customer//("customer") as? org.bson.Document
        assertNotNull(customer)
        assertEquals("Jane Smith", foundCostumer?.name)
    }

    @Test
    fun `should perform pipeline-based lookup`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        mongoTemplate.insertAll(
            listOf(
                OrderItem(id = "i1", orderId = "o1", productId = "p1", quantity = 2, price = 50.0),
                OrderItem(id = "i2", orderId = "o1", productId = "p2", quantity = 1, price = 100.0),
                OrderItem(id = "i3", orderId = "o2", productId = "p1", quantity = 3, price = 50.0)
            )
        )

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 200.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookup<OrderItem> {
                from(OrderItem::class)
                let {
                    "orderId" to Order::id
                }
                pipeline {
                    match<OrderItem> {
                        expr {
                            OrderItem::orderId eq variable("orderId")
                        }
                    }
                    sort {
                        "quantity" from -1
                    }
                }
                into("items")
            }
            .into(OrderWithItems::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        val items = doc.items
        assertNotNull(items)
        assertEquals(2, items.size)
    }

    @Test
    fun `should perform pipeline-based lookup with regular match`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        mongoTemplate.insertAll(
            listOf(
                OrderItem(id = "i1", orderId = "o1", productId = "p1", quantity = 5, price = 50.0),
                OrderItem(id = "i2", orderId = "o1", productId = "p2", quantity = 2, price = 100.0),
                OrderItem(id = "i3", orderId = "o1", productId = "p3", quantity = 1, price = 30.0)
            )
        )

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 380.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When - lookup with quantity filter
        val results = konduct.collection<Order>()
            .lookup<OrderItem> {
                from(OrderItem::class)
                let {
                    "orderId" to Order::id
                }
                pipeline {
                    match<OrderItem> {
                        expr {
                            OrderItem::orderId eq variable("orderId")
                        }
                    }
                    match<OrderItem> {
                        OrderItem::quantity gte 3
                    }
                }
                into("items")
            }
            .into(OrderWithItems::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        val items = doc.items// ("largeItems") as? List<*>
        assertNotNull(items)
        assertEquals(1, items.size) // Only item with quantity >= 3
        assertEquals(true, items.all { it.quantity > 3 })

    }

    @Test
    fun `should perform lookupAndMerge`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        val customer = Customer(id = "c1", name = "Alice Johnson", email = "alice@example.com", tier = "platinum")
        mongoTemplate.insert(customer)

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 500.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookupAndMerge(Customer::class) {
                from()
                on(Order::customerId, Customer::id)
                merge(Customer::name, Customer::email, Customer::tier)
            }
            .into(OrderWithMergedCustomer::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        assertEquals("Alice Johnson", doc.name)
        assertEquals("alice@example.com", doc.email)
        assertEquals("platinum", doc.tier)
    }

    @Test
    fun `should perform lookup with limit in pipeline`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        mongoTemplate.insertAll(
            listOf(
                OrderItem(id = "i1", orderId = "o1", productId = "p1", quantity = 5, price = 10.0),
                OrderItem(id = "i2", orderId = "o1", productId = "p2", quantity = 3, price = 20.0),
                OrderItem(id = "i3", orderId = "o1", productId = "p3", quantity = 2, price = 30.0),
                OrderItem(id = "i4", orderId = "o1", productId = "p4", quantity = 1, price = 40.0)
            )
        )

        val order = Order(
            id = "o1",
            customerId = "c1",
            total = 200.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When - Get top 2 items by quantity
        val results = konduct.collection<Order>()
            .lookup<OrderItem> {
                from(OrderItem::class)
                let {
                    "orderId" to Order::id
                }
                pipeline {
                    match<OrderItem> {
                        expr {
                            OrderItem::orderId eq variable("orderId")
                        }
                    }
                    sort {
                        "quantity" from -1
                    }
                    limit(2)
                }
                into("items")
            }
            .into(OrderWithItems::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        val items = doc.items
        assertNotNull(items)
        assertEquals(2, items.size) // Limited to 2
    }

    @Test
    fun `should handle lookup with no matches`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        val order = Order(
            id = "o1",
            customerId = "nonexistent",
            total = 100.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookup<Customer> {
                from(Customer::class)
                localField(Order::customerId)
                foreignField(Customer::id)
                into("customersInfos")
            }
            .into(OrderWithCustomers::class)
            .toList()

        // Then
        assertEquals(1, results.size)
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        val customerInfo = doc.customersInfos
        assertNotNull(customerInfo)
        assertEquals(0, customerInfo.size)
    }

    @Test
    fun `should unwind with preserveNullAndEmptyArrays`() {
        // Given
        val konduct = Konduct(mongoTemplate)

        val order = Order(
            id = "o1",
            customerId = "nonexistent",
            total = 100.0,
            status = "completed",
            orderDate = Date()
        )
        mongoTemplate.insert(order)

        // When
        val results = konduct.collection<Order>()
            .lookup<Customer> {
                from(Customer::class)
                localField(Order::customerId)
                foreignField(Customer::id)
                into("customer")
            }
            .unwind("customer", preserveNullAndEmptyArrays = true)
            .into(OrderWithCustomer::class)
            .toList()

        // Then
        assertEquals(1, results.size) // Order still present even with no customer match
        val doc = results.getOrNull(0)
        assertNotNull(doc)
        assertNull(doc.customer)
    }


}
