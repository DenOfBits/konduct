 package io.github.denofbits.konduct.examples

import io.github.denofbits.konduct.core.Konduct
import io.github.denofbits.konduct.core.konduct
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.gte
import org.springframework.stereotype.Service

/**
 * Example domain models
 */
@Document("products")
data class Product(
    @Id val id: String? = null,
    val name: String,
    val price: Double,
    val category: String,
    val status: String,
    val rating: Double? = null,
    val inStock: Boolean = true,
    val tags: List<String> = emptyList()
)

@Document("orders")
data class Order(
    @Id val id: String? = null,
    val customerId: String,
    val productId: String,
    val quantity: Int,
    val total: Double,
    val status: String,
    val orderDate: java.util.Date
)

/**
 * Example service demonstrating Konduct usage
 */
@Service
class ProductService(private val mongoTemplate: MongoTemplate) {
    
    private val konduct = Konduct(mongoTemplate)
    
    /**
     * Get all active products
     */
    fun getActiveProducts(): List<Product> {
        return konduct.collection<Product>()
            .match { Product::status eq "active" }
            .toList()
    }
    
    /**
     * Get products in a price range
     */
    fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::price gte minPrice
                Product::price lte maxPrice
                Product::status eq "active"
            }
            .sort { Product::price.asc() }
            .toList()
    }
    
    /**
     * Search products by category with pagination
     */
    fun searchProductsByCategory(
        category: String,
        page: Int,
        pageSize: Int
    ): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::category eq category
                Product::status eq "active"
            }
            .sort { Product::rating.desc() }
            .skip(page * pageSize)
            .limit(pageSize)
            .toList()
    }
    
    /**
     * Get premium products (price > 1000 and rating > 4.5)
     */
    fun getPremiumProducts(): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::status eq "active"
                Product::price gt 1000.0
                Product::rating gte 4.5
            }
            .sort { 
                Product::rating.desc()
                Product::price.desc()
            }
            .toList()
    }
    
    /**
     * Get products by tags
     */
    fun getProductsByTags(tags: List<String>): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::tags `in` tags
                Product::status eq "active"
            }
            .toList()
    }
    
    /**
     * Get out of stock products
     */
    fun getOutOfStockProducts(): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::inStock eq false
            }
            .sort { Product::name.asc() }
            .toList()
    }
    
    /**
     * Find product by ID
     */
    fun findProductById(id: String): Product? {
        return konduct.collection<Product>()
            .match { Product::id eq id }
            .firstOrNull()
    }
    
    /**
     * Count active products
     */
    fun countActiveProducts(): Long {
        return konduct.collection<Product>()
            .match { Product::status eq "active" }
            .count()
    }
    
    /**
     * Get top rated products
     */
    fun getTopRatedProducts(limit: Int = 10): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::status eq "active"
                Product::rating gte 4.0
            }
            .sort { Product::rating.desc() }
            .limit(limit)
            .toList()
    }
}

/**
 * Example service using extension function style
 */
@Service
class OrderService(private val mongoTemplate: MongoTemplate) {
    
    /**
     * Get recent orders for a customer
     */
    fun getCustomerOrders(customerId: String, limit: Int = 20): List<Order> {
        return mongoTemplate.konduct()
            .collection<Order>()
            .match { Order::customerId eq customerId }
            .sort { Order::orderDate.desc() }
            .limit(limit)
            .toList()
    }
    
    /**
     * Get high-value orders
     */
    fun getHighValueOrders(minTotal: Double): List<Order> {
        return mongoTemplate.konduct()
            .collection<Order>()
            .match {
                Order::total gte minTotal
                Order::status eq "completed"
            }
            .sort { Order::total.desc() }
            .toList()
    }
    
    /**
     * Get pending orders
     */
    fun getPendingOrders(): List<Order> {
        return mongoTemplate.konduct()
            .collection<Order>()
            .match { 
                Order::status `in` listOf("pending", "processing") 
            }
            .sort { Order::orderDate.asc() }
            .toList()
    }
    
    /**
     * Count orders by status
     */
    fun countOrdersByStatus(status: String): Long {
        return mongoTemplate.konduct()
            .collection<Order>()
            .match { Order::status eq status }
            .count()
    }
}

/**
 * Example demonstrating complex queries
 */
class AdvancedExamples(private val mongoTemplate: MongoTemplate) {
    
    private val konduct = Konduct(mongoTemplate)
    
    /**
     * Multi-category product search
     */
    fun searchMultipleCategories(categories: List<String>): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::category `in` categories
                Product::status eq "active"
                Product::inStock eq true
            }
            .sort { 
                Product::category.asc()
                Product::price.asc()
            }
            .toList()
    }
    
    /**
     * Products with specific regex pattern in name
     */
    fun searchProductsByNamePattern(pattern: String): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::name regex pattern.toRegex(RegexOption.IGNORE_CASE)
                Product::status eq "active"
            }
            .sort { Product::name.asc() }
            .toList()
    }
    
    /**
     * Get products excluding certain categories
     */
    fun getProductsExcludingCategories(excludedCategories: List<String>): List<Product> {
        return konduct.collection<Product>()
            .match {
                Product::category nin excludedCategories
                Product::status eq "active"
            }
            .toList()
    }
    
    /**
     * Paginated product listing with sorting
     */
    fun getProductsPaginated(
        page: Int,
        pageSize: Int,
        sortBy: String = "name",
        ascending: Boolean = true
    ): List<Product> {
        return konduct.collection<Product>()
            .match { Product::status eq "active" }
            .sort {
                if (ascending) {
                    sortBy.asc()
                } else {
                    sortBy.desc()
                }
            }
            .skip(page * pageSize)
            .limit(pageSize)
            .toList()
    }
}
