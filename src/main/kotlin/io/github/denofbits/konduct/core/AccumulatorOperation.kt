package io.github.denofbits.konduct.core

import io.github.denofbits.konduct.expressions.Expression
import org.bson.Document

sealed class ArrayOperation {
    data class Sum(val value: Any) : ArrayOperation()
    data class SumExpression(val expression: Expression) : ArrayOperation()
    data class SumArray(val field: String) : ArrayOperation()
    data class Avg(val field: String) : ArrayOperation()
    data class AvgExpression(val expression: Expression) : ArrayOperation()
    data class AvgArray(val field: String) : ArrayOperation()
    data class Min(val field: String) : ArrayOperation()
    data class MinArray(val field: String) : ArrayOperation()
    data class Max(val field: String) : ArrayOperation()
    data class MaxArray(val field: String) : ArrayOperation()
    data class First(val field: String) : ArrayOperation()
    data class Last(val field: String) : ArrayOperation()
    object Count : ArrayOperation()
    data class CountDistinct(val field: String) : ArrayOperation()
    data class Push(val field: String) : ArrayOperation()
    data class AddToSet(val field: String) : ArrayOperation()
    data class Size(val field: String) : ArrayOperation()

    fun toMongoExpression(): Any {
        return when (this) {
            is Sum -> Document("\$sum", if (value is Number) value else "\$$value")
            is SumExpression -> Document("\$sum", expression.toMongoExpression())
            is SumArray -> Document("\$sum", "\$$field")
            is Avg -> Document("\$avg", "\$$field")
            is AvgExpression -> Document("\$avg", expression.toMongoExpression())
            is AvgArray -> Document("\$avg", "\$$field")
            is Min -> Document("\$min", "\$$field")
            is MinArray -> Document("\$min", "\$$field")
            is Max -> Document("\$max", "\$$field")
            is MaxArray -> Document("\$max", "\$$field")
            is First -> Document("\$first", "\$$field")
            is Last -> Document("\$last", "\$$field")
            Count -> Document("\$sum", 1)
            is CountDistinct -> Document("\$addToSet", "\$$field")
            is Push -> Document("\$push", "\$$field")
            is AddToSet -> Document("\$addToSet", "\$$field")
            is Size -> Document("\$size", "\$$field")
        }
    }
}