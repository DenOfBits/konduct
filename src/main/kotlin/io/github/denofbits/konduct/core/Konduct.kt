package io.github.denofbits.konduct.core

import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.reflect.KClass

/**
 * Entry point for building MongoDB aggregation pipelines.
 * 
 * Example:
 * ```kotlin
 * val konduct = Konduct(mongoTemplate)
 * val products = konduct.collection<Product>()
 *     .match { Product::status eq "active" }
 *     .toList()
 * ```
 */
class Konduct(val mongoTemplate: MongoTemplate) {
    
    /**
     * Start an aggregation pipeline for the specified collection.
     * 
     * @param T The document type
     * @param collectionName Optional collection name. If not provided, derives from class name.
     * @return A new aggregation pipeline
     */
    inline fun <reified T : Any> collection(collectionName: String? = null): AggregationPipeline<T> {
        val name = collectionName ?: mongoTemplate.getCollectionName(T::class.java)
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = name,
            documentType = T::class,
            stages = mutableListOf()
        )
    }
}

/**
 * Extension function to create Konduct instance from MongoTemplate.
 * 
 * Example:
 * ```kotlin
 * val products = mongoTemplate.konduct()
 *     .collection<Product>()
 *     .match { Product::status eq "active" }
 *     .toList()
 * ```
 */
fun MongoTemplate.konduct(): Konduct = Konduct(this)
