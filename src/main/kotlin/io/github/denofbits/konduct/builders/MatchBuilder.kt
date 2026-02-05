package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.core.Condition
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1
import org.springframework.data.mongodb.core.mapping.Field
import kotlin.reflect.jvm.javaField

/**
 * Builder for $match stage with type-safe field references.
 */
class MatchBuilder<T : Any> {
    private val conditions = mutableListOf<Condition>()

    /*private fun <V> KProperty1<T, V>.getFieldName(): String {
        val field = this.javaField?.getAnnotation(Field::class.java)
        return field?.value ?: this.name
    }*/

    // Equality operators
    infix fun <V> KProperty1<T, V>.eq(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "eq", value))
    }
    
    infix fun <V> KProperty1<T, V>.ne(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "ne", value))
    }
    
    // Comparison operators
    infix fun <V : Comparable<V>> KProperty1<T, V>.gt(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "gt", value))
    }
    
    infix fun <V : Comparable<V>> KProperty1<T, V>.gte(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "gte", value))
    }
    
    infix fun <V : Comparable<V>> KProperty1<T, V>.lt(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "lt", value))
    }
    
    infix fun <V : Comparable<V>> KProperty1<T, V>.lte(value: V) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "lte", value))
    }
    
    // Set operators
    infix fun <V> KProperty1<T, V>.`in`(values: Collection<V>) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "in", values))
    }
    
    infix fun <V> KProperty1<T, V>.nin(values: Collection<V>) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "nin", values))
    }
    
    // String operators
    infix fun KProperty1<T, String>.regex(pattern: Regex) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "regex", pattern.pattern))
    }
    
    infix fun KProperty1<T, String>.regex(pattern: String) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "regex", pattern))
    }
    
    // Existence
    infix fun KProperty1<T, *>.exists(value: Boolean) {
        conditions.add(Condition.FieldCondition(this.getFieldName(), "exists", value))
    }
    
    fun KProperty1<T, *>.isNull() {
        this eq null
    }
    
    fun KProperty1<T, *>.isNotNull() {
        this ne null
    }
    
    // Logical operators
    fun and(vararg conditions: Condition) {
        this.conditions.add(Condition.AndCondition(conditions.toList()))
    }
    
    fun or(vararg conditions: Condition) {
        this.conditions.add(Condition.OrCondition(conditions.toList()))
    }
    
    fun not(condition: Condition) {
        this.conditions.add(Condition.NotCondition(condition))
    }
    
    // String-based fallback for dynamic fields
    infix fun String.eq(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "eq", value))
    }
    
    infix fun String.ne(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "ne", value))
    }
    
    infix fun String.gt(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "gt", value))
    }
    
    infix fun String.gte(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "gte", value))
    }
    
    infix fun String.lt(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "lt", value))
    }
    
    infix fun String.lte(value: Any?) {
        conditions.add(Condition.FieldCondition(this, "lte", value))
    }
    
    /**
     * Build the $match stage from accumulated conditions.
     */
    internal fun build(): AggregationOperation {
        if (conditions.isEmpty()) {
            // Empty match (matches all)
            return Aggregation.match(Criteria())
        }
        
        // Combine all conditions with AND
        val criteria = if (conditions.size == 1) {
            conditions[0].toCriteria()
        } else {
            Criteria().andOperator(*conditions.map { it.toCriteria() }.toTypedArray())
        }
        
        return Aggregation.match(criteria)
    }
}
