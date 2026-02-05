package io.github.denofbits.konduct.core

import io.github.denofbits.konduct.builders.*
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ArrayOperators
import kotlin.reflect.KClass

class AggregationPipelineImpl<T : Any>(
    private val mongoTemplate: MongoTemplate,
    private val collectionName: String,
    private val documentType: KClass<T>,
    private val stages: MutableList<AggregationOperation>,
    private val originalType: KClass<*>? = null
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
        val skipStage = Aggregation.skip(count.toLong())
        return copy(stages = stages + skipStage)
    }

    override fun limit(count: Int): AggregationPipeline<T> {
        val limitStage = Aggregation.limit(count.toLong())
        return copy(stages = stages + limitStage)
    }

    override fun group(block: GroupBuilder<T>.() -> Unit): AggregationPipeline<Document> {
        val builder = GroupBuilder<T>()
        builder.block()
        val groupStages = builder.build()
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = Document::class,
            stages = (stages + groupStages).toMutableList()
        )
    }

    override fun <R : Any> group(resultType: KClass<R>, block: GroupBuilder<T>.() -> Unit): AggregationPipeline<R> {
        val builder = GroupBuilder<T>()
        builder.block()
        val groupStage = builder.build()
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = resultType,
            stages = (stages + groupStage).toMutableList()
        )

    }

    override fun <R : Any> facet(resultType: KClass<R>, block: FacetBuilder<T>.() -> Unit): AggregationPipeline<R> {
        val builder = FacetBuilder<T>()
        builder.block()
        val facetStage = builder.build()
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = resultType,
            stages = (stages + facetStage).toMutableList()
        )
    }

    override fun paginate(page: Int, pageSize: Int): AggregationPipeline<PagedResult<T>> {
        val facetStage = Aggregation.facet(
            Aggregation.skip((page * pageSize).toLong()),
            Aggregation.limit(pageSize.toLong())
        ).`as`("data")
            .and(Aggregation.count().`as`("total")).`as`("metadata")

        val projectStage = Aggregation.project()
            .and("data").`as`("data")
            .and(ArrayOperators.ArrayElemAt.arrayOf("metadata.total").elementAt(0)).`as`("total")

        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = PagedResult::class as KClass<PagedResult<T>>,
            stages = (stages + facetStage + projectStage).toMutableList(),
            originalType = documentType
        )
    }

    override fun <R : Any> into(resultType: KClass<R>): AggregationPipeline<R> {
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = resultType,
            stages = stages.toMutableList()
        )
    }

    override fun toList(): List<T> {
        val aggregation = toAggregation()
        val results = mongoTemplate.aggregate(aggregation, collectionName, documentType.java).mappedResults

        if (documentType == PagedResult::class) {
            val doc = mongoTemplate.aggregate(aggregation, collectionName, Document::class.java).mappedResults.firstOrNull()
            if (doc != null) {
                val data = doc.get("data", List::class.java) as? List<*> ?: emptyList<Any>()
                val total = doc.getLong("total") ?: 0L
                val pagedResult = PagedResult(
                    data = data as List<Any>,
                    total = total,
                    page = 1,
                    pageSize = data.size,
                    totalPages = if (data.isEmpty()) 0 else ((total + data.size - 1) / data.size).toInt()
                )
                return listOf(pagedResult as T)
            }
            return emptyList()
        }

        return results
    }

    override fun firstOrNull(): T? {
        val aggregation = Aggregation.newAggregation(stages + Aggregation.limit(1))
        val typeToUse = originalType ?: documentType

        println(aggregation.toString())
        if (documentType == PagedResult::class) {
            val doc = mongoTemplate.aggregate(aggregation, collectionName, Document::class.java).mappedResults.firstOrNull()
            if (doc != null) {
                val data = doc.get("data", List::class.java) as? List<*> ?: emptyList<Document>()
                val convertedData = data.map {item ->
                    mongoTemplate.converter.read(typeToUse.java, item as Document)
                }
                val total = doc.getInteger("total").toLong() ?: 0L
                val pagedResult = PagedResult(
                    data = convertedData,
                    total = total,
                    page = 1,
                    pageSize = data.size,
                    totalPages = if (data.isEmpty()) 0 else ((total + data.size - 1) / data.size).toInt()
                )
                return pagedResult as T
            }
            return null
        }

        return mongoTemplate.aggregate(aggregation, collectionName, documentType.java).mappedResults.firstOrNull()
    }

    override fun count(): Long {
        val countStage = Aggregation.count().`as`("count")
        val aggregation = Aggregation.newAggregation(stages + countStage)
        val result = mongoTemplate.aggregate(aggregation, collectionName, Document::class.java).mappedResults
        return result.firstOrNull()?.getLong("count") ?: 0L
    }

    override fun toAggregation(): Aggregation {
        return Aggregation.newAggregation(stages)
    }

    override fun toJson(): String {
        val aggregation = toAggregation()
        return aggregation.toString()
    }

    private fun <R : Any> copy(
        stages: List<AggregationOperation>,
        newDocumentType: KClass<R>? = null
    ): AggregationPipeline<R> {
        return AggregationPipelineImpl(
            mongoTemplate = mongoTemplate,
            collectionName = collectionName,
            documentType = newDocumentType ?: documentType as KClass<R>,
            stages = stages.toMutableList()
        )
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
