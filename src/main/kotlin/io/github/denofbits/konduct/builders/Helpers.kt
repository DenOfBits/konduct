package io.github.denofbits.konduct.builders

import org.springframework.data.annotation.Id
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import org.springframework.data.mongodb.core.mapping.Field

fun <V, T> KProperty1<T, V>.getFieldName(): String {
    val field = this.javaField?.getAnnotation(Field::class.java)
    val id = this.javaField?.getAnnotation(Id::class.java)
    //TODO find better approach to check
    return  field?.value ?: id?.toString()?.let { "_id" } ?:  this.name
}