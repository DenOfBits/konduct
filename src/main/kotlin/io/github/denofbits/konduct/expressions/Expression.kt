package io.github.denofbits.konduct.expressions

import kotlin.reflect.KProperty1

sealed interface Expression {
    fun toMongoExpression(): Any
}

data class FieldExpression(val fieldName: String) : Expression {
    override fun toMongoExpression() = "\$$fieldName"
}

data class LiteralExpression(val value: Any) : Expression {
    override fun toMongoExpression() = value
}

data class MultiplyExpression(val left: Expression, val right: Expression) : Expression {
    override fun toMongoExpression() = mapOf("\$multiply" to listOf(left.toMongoExpression(), right.toMongoExpression()))
}

data class AddExpression(val operands: List<Expression>) : Expression {
    override fun toMongoExpression() = mapOf("\$add" to operands.map { it.toMongoExpression() })
}

data class SubtractExpression(val left: Expression, val right: Expression) : Expression {
    override fun toMongoExpression() = mapOf("\$subtract" to listOf(left.toMongoExpression(), right.toMongoExpression()))
}

data class DivideExpression(val numerator: Expression, val denominator: Expression) : Expression {
    override fun toMongoExpression() = mapOf("\$divide" to listOf(numerator.toMongoExpression(), denominator.toMongoExpression()))
}

data class CondExpression(val branches: List<Pair<String, String>>, val default: String) : Expression {
    override fun toMongoExpression() = mapOf(
        "\$switch" to mapOf(
            "branches" to branches.map { mapOf("case" to it.first, "then" to it.second) },
            "default" to default
        )
    )
}