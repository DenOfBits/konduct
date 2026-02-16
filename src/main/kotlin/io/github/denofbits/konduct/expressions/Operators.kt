package io.github.denofbits.konduct.expressions

import io.github.denofbits.konduct.builders.getFieldName
import kotlin.reflect.KProperty1

operator fun <T, V : Number> KProperty1<T, V>.times(other: KProperty1<T, V>): Expression {
    return MultiplyExpression(
        FieldExpression(this.getFieldName()),
        FieldExpression(other.getFieldName())
    )
}

operator fun <T, V : Number> KProperty1<T, V>.times(value: Number): Expression {
    return MultiplyExpression(
        FieldExpression(this.getFieldName()),
        LiteralExpression(value)
    )
}

operator fun <T, V : Number> KProperty1<T, V>.plus(other: KProperty1<T, V>): Expression {
    return AddExpression(listOf(
        FieldExpression(this.getFieldName()),
        FieldExpression(other.getFieldName())
    ))
}

operator fun <T, V : Number> KProperty1<T, V>.minus(other: KProperty1<T, V>): Expression {
    return SubtractExpression(
        FieldExpression(this.getFieldName()),
        FieldExpression(other.getFieldName())
    )
}

operator fun <T, V : Number> KProperty1<T, V>.div(other: KProperty1<T, V>): Expression {
    return DivideExpression(
        FieldExpression(this.getFieldName()),
        FieldExpression(other.getFieldName())
    )
}

operator fun <T, V : Number> KProperty1<T, V>.div(value: Number): Expression {
    return DivideExpression(
        FieldExpression(this.getFieldName()),
        LiteralExpression(value)
    )
}

operator fun Expression.times(other: Expression): Expression {
    return MultiplyExpression(this, other)
}

operator fun Expression.plus(other: Expression): Expression {
    return AddExpression(listOf(this, other))
}

operator fun Expression.minus(other: Expression): Expression {
    return SubtractExpression(this, other)
}

operator fun Expression.div(other: Expression): Expression {
    return DivideExpression(this, other)
}