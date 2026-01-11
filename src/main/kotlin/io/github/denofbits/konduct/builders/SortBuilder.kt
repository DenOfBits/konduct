package io.github.denofbits.konduct.builders

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KProperty1

/**
 * Builder for $sort stage with type-safe field references.
 */
class SortBuilder<T : Any> {
    private val sorts = mutableListOf<Pair<String, Sort.Direction>>()
    
    /**
     * Sort field in ascending order.
     */
    /*infix fun KProperty1<T, *>.asc(unit: Unit) {
        sorts.add(this.name to Sort.Direction.ASC)
    }*/
    
    /**
     * Sort field in descending order.
     */
    /*infix fun KProperty1<T, *>.desc(unit: Unit) {
        sorts.add(this.name to Sort.Direction.DESC)
    }*/

    fun KProperty1<T, *>.asc() {
        sorts.add(this.name to Sort.Direction.ASC)
    }

    fun KProperty1<T, *>.desc() {
        sorts.add(this.name to Sort.Direction.DESC)
    }

    fun String.asc() {
        sorts.add(this to Sort.Direction.ASC)
    }

    fun String.desc() {
        sorts.add(this to Sort.Direction.DESC)
    }
    
    /**
     * String-based field sorting (fallback for dynamic fields).
     */
    infix fun String.asc(unit: Unit) {
        sorts.add(this to Sort.Direction.ASC)
    }
    
    infix fun String.desc(unit: Unit) {
        sorts.add(this to Sort.Direction.DESC)
    }
    
    /**
     * Sort multiple fields in ascending order.
     */
    fun ascending(vararg fields: KProperty1<T, *>) {
        fields.forEach { sorts.add(it.name to Sort.Direction.ASC) }
    }
    
    /**
     * Sort multiple fields in descending order.
     */
    fun descending(vararg fields: KProperty1<T, *>) {
        fields.forEach { sorts.add(it.name to Sort.Direction.DESC) }
    }
    
    /**
     * Build the $sort stage from accumulated sort orders.
     */
    internal fun build(): AggregationOperation {
        if (sorts.isEmpty()) {
            throw IllegalStateException("Sort builder has no sort criteria")
        }
        
        val sortOrders = sorts.map { (field, direction) ->
            Sort.Order(direction, field)
        }
        
        return Aggregation.sort(Sort.by(sortOrders))
    }
}
