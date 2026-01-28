package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.core.CustomAggregationOperation
import io.github.denofbits.konduct.expressions.Expression
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import kotlin.reflect.KProperty1

class GroupBuilder<T : Any> {
    private var groupByField: Any? = null
    private val accumulators = mutableMapOf<String, AccumulatorOperation>()

    // Simple field grouping
    fun by(field: KProperty1<T, *>) {
        groupByField = field.getFieldName()
    }

    fun by(fieldName: String) {
        groupByField = fieldName
    }

    // Composite key grouping
    fun by(block: CompositeKeyBuilder<T>.() -> Unit) {
        val builder = CompositeKeyBuilder<T>()
        builder.block()
        groupByField = builder
    }

    fun accumulate(block: AccumulateBuilder<T>.() -> Unit) {
        val builder = AccumulateBuilder<T>()
        builder.block()
        accumulators.putAll(builder.build())
    }

    internal fun build(): List<AggregationOperation> {
        val groupBy = groupByField ?: throw IllegalStateException("Group by field not specified")

        val groupDoc = Document()

        // Handle _id field
        when (groupBy) {
            is String -> groupDoc["_id"] = "\$$groupBy"
            is Map<*, *> -> {
                val idDoc = Document()
                (groupBy as Map<String, Any>).forEach { (key, value) ->
                    idDoc[key] = if (value is String) "\$$value" else value
                }
                groupDoc["_id"] = idDoc
            }
            is CompositeKeyBuilder<*> -> {
                val idDoc = Document()
                (groupBy as CompositeKeyBuilder<T>).build().forEach { (key, value) ->
                    idDoc[key] = if (value is String) "\$$value" else value
                }
                groupDoc["_id"] = idDoc
            }
            else -> groupDoc["_id"] = groupBy
        }

        // Handle accumulators
        accumulators.forEach { (fieldName, operation) ->
            groupDoc[fieldName] = when (operation) {
                is AccumulatorOperation.Sum -> {
                    Document("\$sum", if (operation.value is Number) operation.value else "\$${operation.value}")
                }
                is AccumulatorOperation.SumExpression -> {
                    Document("\$sum", operation.expression.toMongoExpression())
                }
                is AccumulatorOperation.Avg -> Document("\$avg", "\$${operation.field}")
                is AccumulatorOperation.AvgExpression -> Document("\$avg", operation.expression.toMongoExpression())
                is AccumulatorOperation.Min -> Document("\$min", "\$${operation.field}")
                is AccumulatorOperation.Max -> Document("\$max", "\$${operation.field}")
                is AccumulatorOperation.First -> Document("\$first", "\$${operation.field}")
                is AccumulatorOperation.Last -> Document("\$last", "\$${operation.field}")
                is AccumulatorOperation.Count -> Document("\$sum", 1)
                is AccumulatorOperation.CountDistinct -> Document("\$addToSet", "\$${operation.field}")
                is AccumulatorOperation.Push -> Document("\$push", "\$${operation.field}")
                is AccumulatorOperation.AddToSet -> Document("\$addToSet", "\$${operation.field}")
            }
        }

        val stages = mutableListOf<AggregationOperation>()
        stages.add(CustomAggregationOperation(Document("\$group", groupDoc)))

        // ADD FIELDS stage to extract grouped fields from _id
        val addFieldsDoc = Document()
        when (val groupBy = groupByField) {
            is String -> {
                addFieldsDoc[groupBy] = "\$_id"
            }
            is CompositeKeyBuilder<*> -> {
                (groupBy as CompositeKeyBuilder<T>).build().forEach { (key, _) ->
                    addFieldsDoc[key] = "\$_id.$key"
                }
            }
        }

        if (addFieldsDoc.isNotEmpty()) {
            stages.add(CustomAggregationOperation(Document("\$addFields", addFieldsDoc)))
        }

        return stages
        //return CustomAggregationOperation(Document("\$group", groupDoc))
    }

    /*internal fun build(): AggregationOperation {
        when (val groupBy = groupByField) {
            null -> throw IllegalStateException("Group by field not specified")
            is String -> {
                var groupOp = Aggregation.group(groupBy)
                accumulators.forEach { (fieldName, operation) ->
                    groupOp = applyAccumulator(groupOp, fieldName, operation)
                }
                return groupOp
            }
            is CompositeKeyBuilder<*> -> {
                val fields = (groupBy as CompositeKeyBuilder<T>).build()
                var groupOp = Aggregation.group(*fields.keys.toTypedArray())

                fields.forEach { (key, value) ->
                    when (value) {
                        is String -> groupOp = groupOp.and(value).`as`(key)
                        // Add more complex field types here if needed
                    }
                }

                accumulators.forEach { (fieldName, operation) ->
                    groupOp = applyAccumulator(groupOp, fieldName, operation)
                }
                return groupOp
            }
            else -> throw IllegalStateException("Unsupported group by type")
        }
    }*/

    /*private fun applyAccumulator(
        groupOp: GroupOperation,
        fieldName: String,
        operation: AccumulatorOperation
    ): GroupOperation {
        return when (operation) {
            is AccumulatorOperation.Sum -> {
                if (operation.value is Number) {
                    groupOp.sum(operation.value.toString()).`as`(fieldName)
                } else {
                    groupOp.sum(operation.value as String).`as`(fieldName)
                }
            }
            is AccumulatorOperation.SumExpression -> {  // ADD
                val customOp = CustomAggregationOperation(
                    Document("\$sum", operation.expression.toMongoExpression())
                )
                groupOp.sum(operation.expression.toMongoExpression()).`as`(fieldName)
            }
            is AccumulatorOperation.AvgExpression -> {  // ADD
                groupOp.avg(operation.expression.toMongoExpression()).`as`(fieldName)
            }
            is AccumulatorOperation.Avg -> groupOp.avg(operation.field).`as`(fieldName)
            is AccumulatorOperation.Min -> groupOp.min(operation.field).`as`(fieldName)
            is AccumulatorOperation.Max -> groupOp.max(operation.field).`as`(fieldName)
            is AccumulatorOperation.First -> groupOp.first(operation.field).`as`(fieldName)
            is AccumulatorOperation.Last -> groupOp.last(operation.field).`as`(fieldName)
            is AccumulatorOperation.Count -> groupOp.count().`as`(fieldName)
            is AccumulatorOperation.CountDistinct -> groupOp.addToSet(operation.field).`as`(fieldName)
            is AccumulatorOperation.Push -> groupOp.push(operation.field).`as`(fieldName)
            is AccumulatorOperation.AddToSet -> groupOp.addToSet(operation.field).`as`(fieldName)
        }
    }*/
}

class CompositeKeyBuilder<T : Any> {
    private val fields = mutableMapOf<String, Any>()

    infix fun String.from(field: KProperty1<T, *>) {
        fields[this] = field.getFieldName()
    }

    infix fun String.from(fieldName: String) {
        fields[this] = fieldName
    }

    internal fun build(): Map<String, Any> = fields
}

class AccumulateBuilder<T : Any> {
    private val operations = mutableMapOf<String, AccumulatorOperation>()

    infix fun String.sum(field: KProperty1<T, Number>) {
        operations[this] = AccumulatorOperation.Sum(field.getFieldName())
    }

    infix fun String.sum(fieldName: String) {
        operations[this] = AccumulatorOperation.Sum(fieldName)
    }

    infix fun String.sum(value: Number) {
        operations[this] = AccumulatorOperation.Sum(value)
    }

    infix fun String.avg(field: KProperty1<T, Number>) {
        operations[this] = AccumulatorOperation.Avg(field.getFieldName())
    }

    infix fun String.avg(fieldName: String) {
        operations[this] = AccumulatorOperation.Avg(fieldName)
    }

    infix fun String.min(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.Min(field.getFieldName())
    }

    infix fun String.min(fieldName: String) {
        operations[this] = AccumulatorOperation.Min(fieldName)
    }

    infix fun String.max(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.Max(field.getFieldName())
    }

    infix fun String.max(fieldName: String) {
        operations[this] = AccumulatorOperation.Max(fieldName)
    }

    infix fun String.first(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.First(field.getFieldName())
    }

    infix fun String.first(fieldName: String) {
        operations[this] = AccumulatorOperation.First(fieldName)
    }

    infix fun String.last(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.Last(field.getFieldName())
    }

    infix fun String.last(fieldName: String) {
        operations[this] = AccumulatorOperation.Last(fieldName)
    }

    infix fun String.count(unit: Unit) {
        operations[this] = AccumulatorOperation.Count
    }

    infix fun String.countDistinct(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.CountDistinct(field.getFieldName())
    }

    infix fun String.countDistinct(fieldName: String) {
        operations[this] = AccumulatorOperation.CountDistinct(fieldName)
    }

    infix fun String.push(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.Push(field.getFieldName())
    }

    infix fun String.push(fieldName: String) {
        operations[this] = AccumulatorOperation.Push(fieldName)
    }

    infix fun String.addToSet(field: KProperty1<T, *>) {
        operations[this] = AccumulatorOperation.AddToSet(field.getFieldName())
    }

    infix fun String.addToSet(fieldName: String) {
        operations[this] = AccumulatorOperation.AddToSet(fieldName)
    }

    infix fun String.sum(expression: Expression) {
        operations[this] = AccumulatorOperation.SumExpression(expression)
    }

    infix fun String.avg(expression: Expression) {
        operations[this] = AccumulatorOperation.AvgExpression(expression)
    }

    internal fun build(): Map<String, AccumulatorOperation> = operations
}

sealed class AccumulatorOperation {

    data class Sum(val value: Any) : AccumulatorOperation()
    data class SumExpression(val expression: Expression) : AccumulatorOperation()
    data class Avg(val field: String) : AccumulatorOperation()
    data class AvgExpression(val expression: Expression) : AccumulatorOperation()
    data class Min(val field: String) : AccumulatorOperation()
    data class Max(val field: String) : AccumulatorOperation()
    data class First(val field: String) : AccumulatorOperation()
    data class Last(val field: String) : AccumulatorOperation()
    object Count : AccumulatorOperation()
    data class CountDistinct(val field: String) : AccumulatorOperation()
    data class Push(val field: String) : AccumulatorOperation()
    data class AddToSet(val field: String) : AccumulatorOperation()
}