package io.github.denofbits.konduct.core

import org.springframework.data.mongodb.core.query.Criteria

/**
 * Represents a match condition that can be converted to MongoDB Criteria.
 */
sealed class Condition {
    abstract fun toCriteria(): Criteria
    
    /**
     * Single field condition (e.g., field eq value)
     */
    data class FieldCondition(
        val fieldName: String,
        val operator: String,
        val value: Any?
    ) : Condition() {
        override fun toCriteria(): Criteria {
            val criteria = Criteria.where(fieldName)
            return when (operator) {
                "eq" -> criteria.`is`(value)
                "ne" -> criteria.ne(value)
                "gt" -> criteria.gt(value)
                "gte" -> criteria.gte(value)
                "lt" -> criteria.lt(value)
                "lte" -> criteria.lte(value)
                "in" -> criteria.`in`(value as Collection<*>)
                "nin" -> criteria.nin(value as Collection<*>)
                "exists" -> if (value as Boolean) criteria.exists(true) else criteria.exists(false)
                "regex" -> criteria.regex(value.toString())
                else -> throw IllegalArgumentException("Unknown operator: $operator")
            }
        }
    }
    
    /**
     * Logical AND condition
     */
    data class AndCondition(val conditions: List<Condition>) : Condition() {
        override fun toCriteria(): Criteria {
            val criteriaList = conditions.map { it.toCriteria() }
            return Criteria().andOperator(*criteriaList.toTypedArray())
        }
    }
    
    /**
     * Logical OR condition
     */
    data class OrCondition(val conditions: List<Condition>) : Condition() {
        override fun toCriteria(): Criteria {
            val criteriaList = conditions.map { it.toCriteria() }
            return Criteria().orOperator(*criteriaList.toTypedArray())
        }
    }
    
    /**
     * Logical NOT condition
     */
    data class NotCondition(val condition: Condition) : Condition() {
        override fun toCriteria(): Criteria {
            return Criteria().norOperator(condition.toCriteria())
        }
    }
}
