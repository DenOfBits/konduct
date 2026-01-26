package io.github.denofbits.konduct.builders

import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.GroupOperation
import kotlin.reflect.KProperty1

class GroupBuilder<T : Any> {
    private var groupByField: String? = null
    private val accumulators = mutableMapOf<String, AccumulatorOperation>()
    
    fun by(field: KProperty1<T, *>) {
        groupByField = field.getFieldName()
    }
    
    fun by(fieldName: String) {
        groupByField = fieldName
    }
    
    fun accumulate(block: AccumulateBuilder<T>.() -> Unit) {
        val builder = AccumulateBuilder<T>()
        builder.block()
        accumulators.putAll(builder.build())
    }
    
    internal fun build(): AggregationOperation {
        val groupBy = groupByField ?: throw IllegalStateException("Group by field not specified")
        
        var groupOp = Aggregation.group(groupBy)
        
        accumulators.forEach { (fieldName, operation) ->
            groupOp = when (operation) {
                is AccumulatorOperation.Sum -> {
                    if (operation.value is Number) {
                        groupOp.sum(operation.value.toString()).`as`(fieldName)
                    } else {
                        groupOp.sum(operation.value as String).`as`(fieldName)
                    }
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
        }
        
        return groupOp
    }
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
    
    internal fun build(): Map<String, AccumulatorOperation> = operations
}

sealed class AccumulatorOperation {
    data class Sum(val value: Any) : AccumulatorOperation()
    data class Avg(val field: String) : AccumulatorOperation()
    data class Min(val field: String) : AccumulatorOperation()
    data class Max(val field: String) : AccumulatorOperation()
    data class First(val field: String) : AccumulatorOperation()
    data class Last(val field: String) : AccumulatorOperation()
    object Count : AccumulatorOperation()
    data class CountDistinct(val field: String) : AccumulatorOperation()
    data class Push(val field: String) : AccumulatorOperation()
    data class AddToSet(val field: String) : AccumulatorOperation()
}
