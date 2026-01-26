package io.github.denofbits.konduct.builders

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import org.springframework.data.mongodb.core.mapping.Field

fun <V, T> KProperty1<T, V>.getFieldName(): String {
    val field = this.javaField?.getAnnotation(Field::class.java)
    return field?.value ?: this.name
}