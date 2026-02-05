package io.github.denofbits.konduct.core

import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext

class CustomAggregationOperation(private val operation: Document) : AggregationOperation {

    @Deprecated("Deprecated in Java")
    override fun toDocument(context: AggregationOperationContext): Document {
        return context.getMappedObject(operation)
    }

    override fun getOperator(): String {
        return "none"
    }

    fun getDocument() = operation
}