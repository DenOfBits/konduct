package io.github.denofbits.konduct.core

import io.github.denofbits.konduct.builders.MatchBuilder
import io.github.denofbits.konduct.builders.SortBuilder
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KClass

/**
 * Implementation of AggregationPipeline.
 * 
 * Maintains immutability by creating new instances for each operation.
 */
class AggregationPipelineImpl<T : Any>(
    private val mongoTemplate: MongoTemplate,
    private val collectionName: String,
    private val documentType: KClass<T>,
    private val stages: MutableList<AggregationOperation>
) : AggregationPipeline<T> {
    
    override fun match(block: MatchBuilder<T>.() -> Unit): AggregationPipeline<T> {
        val builder = MatchBuilder<T>()
        builder.block()
        val matchStage = builder.build()
        return copy(stages = stages + matchStage)
    }
    
    override fun sort(block: SortBuilder<T>.() -> Unit): AggregationPipeline<T> {
        val builder = SortBuilder<T>()
        builder.block()
        val sortStage = builder.build()
        return copy(stages = stages + sortStage)
    }
    
    override fun skip(count: Int): AggregationPipeline<T> {
        val skipStage = org.springframework.data.mongodb.core.aggregation.Aggregation.skip(count.toLong())
        return copy(stages = stages + skipStage)
    }
    
    override fun limit(count: Int): AggregationPipeline<T> {
        val limitStage = org.springframework.data.mongodb.core.aggregation.Aggregation.limit(count.toLong())
        return copy(stages = stages + limitStage)
    }
    
    override fun toList(): List<T> {
        val aggregation = toAggregation()
        return mongoTemplate.aggregate(aggregation, collectionName, documentType.java).mappedResults
    }
    
    override fun firstOrNull(): T? {
        val aggregation = Aggregation.newAggregation(stages + org.springframework.data.mongodb.core.aggregation.Aggregation.limit(1))
        val results = mongoTemplate.aggregate(aggregation, collectionName, documentType.java).mappedResults
        return results.firstOrNull()
    }
    
    override fun count(): Long {
        val countStage = org.springframework.data.mongodb.core.aggregation.Aggregation.count().`as`("count")
        val aggregation = Aggregation.newAggregation(stages + countStage)
        val result = mongoTemplate.aggregate(aggregation, collectionName, org.bson.Document::class.java).mappedResults
        return result.firstOrNull()?.getLong("count") ?: 0L
    }
    
    override fun toAggregation(): Aggregation {
        return Aggregation.newAggregation(stages)
    }
    
    override fun toJson(): String {
        val aggregation = toAggregation()
        return aggregation.toString()
    }
    
    private fun copy(stages: List<AggregationOperation>): AggregationPipeline<T> {
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = documentType,
            stages = stages.toMutableList()
        )
    }
}
