package io.github.denofbits.konduct.core

import io.github.denofbits.konduct.builders.FacetBuilder
import io.github.denofbits.konduct.builders.GroupBuilder
import io.github.denofbits.konduct.builders.MatchBuilder
import io.github.denofbits.konduct.builders.SortBuilder
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import kotlin.reflect.KClass

/**
 * Fluent interface for building MongoDB aggregation pipelines.
 * 
 * Each operation returns a new pipeline instance (immutable pattern).
 */
interface AggregationPipeline<T : Any> {
    
    /**
     * Filter documents ($match stage).
     * 
     * Example:
     * ```kotlin
     * .match { 
     *     Product::status eq "active"
     *     Product::price gte 100
     * }
     * ```
     */
    fun match(block: MatchBuilder<T>.() -> Unit): AggregationPipeline<T>
    
    /**
     * Sort documents ($sort stage).
     * 
     * Example:
     * ```kotlin
     * .sort { 
     *     Product::price asc
     *     Product::name desc
     * }
     * ```
     */
    fun sort(block: SortBuilder<T>.() -> Unit): AggregationPipeline<T>
    
    /**
     * Skip N documents ($skip stage).
     */
    fun skip(count: Int): AggregationPipeline<T>
    
    /**
     * Limit to N documents ($limit stage).
     */
    fun limit(count: Int): AggregationPipeline<T>

    fun group(block: GroupBuilder<T>.() -> Unit): AggregationPipeline<Document>

    fun <R : Any> group(resultType: KClass<R>, block: GroupBuilder<T>.() -> Unit): AggregationPipeline<R>

    fun <R : Any> facet(resultType: KClass<R>, block: FacetBuilder<T>.() -> Unit): AggregationPipeline<R>

    /**
     * Paginate result of previous stages output
     */
    fun paginate(page: Int, pageSize: Int): AggregationPipeline<PagedResult<T>>

    fun <R : Any> into(resultType: KClass<R>): AggregationPipeline<R>

    /**
     * Execute pipeline and return all results.
     */
    fun toList(): List<T>
    
    /**
     * Execute pipeline and return first result or null.
     */
    fun firstOrNull(): T?
    
    /**
     * Execute pipeline and return count of matching documents.
     */
    fun count(): Long
    
    /**
     * Get raw Spring Data Aggregation for debugging.
     */
    fun toAggregation(): Aggregation
    
    /**
     * Get pipeline as JSON string for debugging.
     */
    fun toJson(): String
}
