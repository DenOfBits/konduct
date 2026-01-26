package io.github.denofbits.konduct.builders

import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.FacetOperation

class FacetBuilder<T : Any> {
    private val facets = mutableMapOf<String, List<AggregationOperation>>()
    
    infix fun String.performs(block: FacetPipelineBuilder<T>.() -> Unit) {
        val builder = FacetPipelineBuilder<T>()
        builder.block()
        facets[this] = builder.build()
    }
    
    internal fun build(): AggregationOperation {
        if (facets.isEmpty()) {
            throw IllegalStateException("Facet builder has no facets defined")
        }
        
        var facetOp: FacetOperation? = null
        
        facets.forEach { (name, operations) ->
            facetOp = if (facetOp == null) {
                Aggregation.facet(*operations.toTypedArray()).`as`(name)
            } else {
                facetOp!!.and(*operations.toTypedArray()).`as`(name)
            }
        }
        
        return facetOp!!
    }
}

class FacetPipelineBuilder<T : Any> {
    private val stages = mutableListOf<AggregationOperation>()
    
    fun match(block: MatchBuilder<T>.() -> Unit) {
        val builder = MatchBuilder<T>()
        builder.block()
        stages.add(builder.build())
    }
    
    fun sort(block: SortBuilder<T>.() -> Unit) {
        val builder = SortBuilder<T>()
        builder.block()
        stages.add(builder.build())
    }
    
    fun skip(count: Int) {
        stages.add(Aggregation.skip(count.toLong()))
    }
    
    fun limit(count: Int) {
        stages.add(Aggregation.limit(count.toLong()))
    }
    
    fun group(block: GroupBuilder<T>.() -> Unit) {
        val builder = GroupBuilder<T>()
        builder.block()
        stages.add(builder.build())
    }
    
    fun count() {
        stages.add(Aggregation.count().`as`("count"))
    }
    
    internal fun build(): List<AggregationOperation> = stages
}
