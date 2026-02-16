package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.expressions.Expression
import org.bson.Document
import kotlin.reflect.KProperty1

class ExprBuilder<T : Any> {
    private var expression: Any? = null

    // Variable reference
    fun variable(name: String): String = "$$${name}"

    fun field(name: String): String = "\$$name"

    fun <V> field(property: KProperty1<T, V>): String = "\$${property.getFieldName()}"

    // Comparison operators
    infix fun <V> KProperty1<T, V>.eq(varName: String): ExprBuilder<T> {
        expression = Document("\$eq", listOf("\$${this.getFieldName()}", varName))
        return this@ExprBuilder
    }

    infix fun String.eq(value: Any): ExprBuilder<T> {
        expression = Document("\$eq", listOf(this, value))
        return this@ExprBuilder
    }

    infix fun <V> KProperty1<T, V>.gt(value: Any): ExprBuilder<T> {
        expression = Document("\$gt", listOf("\$${this.getFieldName()}", value))
        return this@ExprBuilder
    }

    infix fun String.gt(value: Any): ExprBuilder<T> {
        expression = Document("\$gt", listOf(this, value))
        return this@ExprBuilder
    }

    infix fun <V> KProperty1<T, V>.gte(value: Any): ExprBuilder<T> {
        expression = Document("\$gte", listOf("\$${this.getFieldName()}", value))
        return this@ExprBuilder
    }

    infix fun String.gte(value: Any): ExprBuilder<T> {
        expression = Document("\$gte", listOf(this, value))
        return this@ExprBuilder
    }

    infix fun <V> KProperty1<T, V>.lt(value: Any): ExprBuilder<T> {
        expression = Document("\$lt", listOf("\$${this.getFieldName()}", value))
        return this@ExprBuilder
    }

    infix fun String.lt(value: Any): ExprBuilder<T> {
        expression = Document("\$lt", listOf(this, value))
        return this@ExprBuilder
    }

    infix fun <V> KProperty1<T, V>.lte(value: Any): ExprBuilder<T> {
        expression = Document("\$lte", listOf("\$${this.getFieldName()}", value))
        return this@ExprBuilder
    }

    infix fun String.lte(value: Any): ExprBuilder<T> {
        expression = Document("\$lte", listOf(this, value))
        return this@ExprBuilder
    }

    // Logical operators
    fun and(vararg expressions: Any): ExprBuilder<T> {
        expression = Document("\$and", expressions.toList())
        return this
    }

    fun or(vararg expressions: Any): ExprBuilder<T> {
        expression = Document("\$or", expressions.toList())
        return this
    }

    fun not(expr: Any): ExprBuilder<T> {
        expression = Document("\$not", expr)
        return this
    }

    // in operator for variables
    fun `in`(field: String, array: String): ExprBuilder<T> {
        expression = Document("\$in", listOf(field, array))
        return this
    }

    // Use Expression objects
    infix fun String.from(expr: Expression): ExprBuilder<T> {
        expression = expr.toMongoExpression()
        return this@ExprBuilder
    }

    fun expr(value: Any): ExprBuilder<T> {
        expression = value
        return this
    }

    internal fun build(): Any = expression ?: throw IllegalStateException("No expression defined")
}