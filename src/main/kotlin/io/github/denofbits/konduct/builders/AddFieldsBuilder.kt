package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.core.CustomAggregationOperation
import io.github.denofbits.konduct.expressions.Expression
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KProperty1

class AddFieldsBuilder<T : Any> {
    private val fields = mutableMapOf<String, Any>()

    infix fun String.from(field: KProperty1<T, *>) {
        fields[this] = "\$${field.getFieldName()}"
    }

    infix fun String.from(fieldName: String) {
        fields[this] = if (fieldName.startsWith("$")) fieldName else "\$$fieldName"
    }

    infix fun String.from(value: Any) {
        fields[this] = value
    }

    infix fun String.from(expression: Expression) {
        fields[this] = expression.toMongoExpression()
    }

    // For accumulator operations on arrays
    infix fun String.sumOf(field: KProperty1<T, *>) {
        fields[this] = Document("\$sum", "\$${field.getFieldName()}")
    }

    infix fun String.sumOf(fieldName: String) {
        fields[this] = Document("\$sum", if (fieldName.startsWith("$")) fieldName else "\$$fieldName")
    }

    infix fun String.avgOf(field: KProperty1<T, *>) {
        fields[this] = Document("\$avg", "\$${field.getFieldName()}")
    }

    infix fun String.avgOf(fieldName: String) {
        fields[this] = Document("\$avg", if (fieldName.startsWith("$")) fieldName else "\$$fieldName")
    }

    infix fun String.maxOf(field: KProperty1<T, *>) {
        fields[this] = Document("\$max", "\$${field.getFieldName()}")
    }

    infix fun String.minOf(field: KProperty1<T, *>) {
        fields[this] = Document("\$min", "\$${field.getFieldName()}")
    }

    infix fun String.sizeOf(field: KProperty1<T, *>) {
        fields[this] = Document("\$size", "\$${field.getFieldName()}")
    }

    // String concatenation helper
    infix fun String.concat(parts: List<Any>) {
        val mongoParts = parts.map { part ->
            when (part) {
                is KProperty1<*, *> -> "\$${part.getFieldName()}"
                is String -> if (part.startsWith("$")) part else part
                else -> part
            }
        }
        fields[this] = Document("\$concat", mongoParts)
    }

    internal fun build(): AggregationOperation {
        val addFieldsDoc = Document()
        fields.forEach { (key, value) ->
            addFieldsDoc[key] = value
        }
        return CustomAggregationOperation(Document("\$addFields", addFieldsDoc))
    }
}
