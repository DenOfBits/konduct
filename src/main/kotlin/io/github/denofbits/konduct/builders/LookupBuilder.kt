package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.core.CustomAggregationOperation
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class LookupBuilder<T : Any> {
    internal var fromCollection: String? = null
    private var localField: String? = null
    private var foreignField: String? = null
    private var asField: String? = null
    private val letVars = mutableMapOf<String, String>()
    private var pipeline: List<Document>? = null

    // Simple lookup
    internal inline fun <reified F : Any> from() {
        fromCollection = F::class.simpleName?.lowercase() + "s"
    }

    fun from(collectionName: String) {
        fromCollection = collectionName
    }

    fun <F : Any> from(klass: KClass<F>) {
        fromCollection = klass.simpleName?.lowercase() + "s"
    }

    fun localField(field: KProperty1<T, *>) {
        localField = field.getFieldName()
    }

    fun localField(fieldName: String) {
        localField = fieldName
    }

    fun <F : Any> foreignField(field: KProperty1<F, *>) {
        foreignField = field.getFieldName()
    }

    fun foreignField(fieldName: String) {
        foreignField = fieldName
    }

    infix fun String.into(asFieldName: String) {
        asField = asFieldName
    }

    // Pipeline-based lookup
    fun let(block: LetBuilder<T>.() -> Unit) {
        val builder = LetBuilder<T>()
        builder.block()
        letVars.putAll(builder.build())
    }

    fun pipeline(block: LookupPipelineBuilder.() -> Unit) {
        val builder = LookupPipelineBuilder()
        builder.block()
        pipeline = builder.build()
    }

    fun into(asFieldName: String) {
        asField = asFieldName
    }

    internal fun build(): AggregationOperation {
        val fromColl = fromCollection ?: throw IllegalStateException("from() collection not specified")
        val asF = asField ?: throw IllegalStateException("into() field not specified")

        val lookupDoc = Document()
        lookupDoc["from"] = fromColl
        lookupDoc["as"] = asF

        if (pipeline != null) {
            // Pipeline-based lookup
            if (letVars.isNotEmpty()) {
                lookupDoc["let"] = Document(letVars)
            }
            lookupDoc["pipeline"] = pipeline
        } else {
            // Simple lookup
            val localF = localField ?: throw IllegalStateException("localField() not specified")
            val foreignF = foreignField ?: throw IllegalStateException("foreignField() not specified")

            lookupDoc["localField"] = localF
            lookupDoc["foreignField"] = foreignF
        }

        return CustomAggregationOperation(Document("\$lookup", lookupDoc))
    }
}

class LetBuilder<T : Any> {
    private val vars = mutableMapOf<String, String>()

    infix fun String.to(field: KProperty1<T, *>) {
        vars[this] = "\$${field.getFieldName()}"
    }

    infix fun String.to(fieldName: String) {
        vars[this] = if (fieldName.startsWith("$")) fieldName else "\$$fieldName"
    }

    internal fun build(): Map<String, String> = vars
}

class LookupPipelineBuilder {
    private val stages = mutableListOf<Document>()

    fun <T : Any> match(block: LookupMatchBuilder<T>.() -> Unit) {
        val builder = LookupMatchBuilder<T>()
        builder.block()
        stages.add(builder.build())
    }

    fun sort(block: DocBuilder.() -> Unit) {
        val builder = DocBuilder()
        builder.block()
        stages.add(Document("\$sort", builder.build()))
    }

    fun limit(count: Int) {
        stages.add(Document("\$limit", count))
    }

    fun skip(count: Int) {
        stages.add(Document("\$skip", count))
    }

    fun project(block: DocBuilder.() -> Unit) {
        val builder = DocBuilder()
        builder.block()
        stages.add(Document("\$project", builder.build()))
    }

    internal fun build(): List<Document> = stages
}

class LookupMatchBuilder<T : Any> {
    private val conditions = mutableMapOf<String, Any>()
    private var exprCondition: Any? = null

    // Regular field conditions
    infix fun <V> KProperty1<T, V>.eq(value: Any) {
        conditions[this.getFieldName()] = value
    }

    infix fun String.eq(value: Any) {
        conditions[this] = value
    }

    infix fun <V> KProperty1<T, V>.gt(value: Any) {
        conditions[this.getFieldName()] = Document("\$gt", value)
    }

    infix fun <V> KProperty1<T, V>.gte(value: Any) {
        conditions[this.getFieldName()] = Document("\$gte", value)
    }

    infix fun <V> KProperty1<T, V>.lt(value: Any) {
        conditions[this.getFieldName()] = Document("\$lt", value)
    }

    infix fun <V> KProperty1<T, V>.lte(value: Any) {
        conditions[this.getFieldName()] = Document("\$lte", value)
    }

    // Expression support for variables
    fun expr(block: ExprBuilder<T>.() -> Unit) {
        val builder = ExprBuilder<T>()
        builder.block()
        exprCondition = builder.build()
    }

    internal fun build(): Document {
        return if (exprCondition != null) {
            Document("\$match", Document("\$expr", exprCondition))
        } else {
            Document("\$match", Document(conditions))
        }
    }
}

class DocBuilder {
    private val doc = Document()

    infix fun String.from(value: Any) {
        doc[this] = value
    }

    infix fun String.from(block: DocBuilder.() -> Unit) {
        val builder = DocBuilder()
        builder.block()
        doc[this] = builder.build()
    }

    fun eq(field: String, value: Any): Document {
        return Document("\$eq", listOf(field, value))
    }

    fun gt(field: String, value: Any): Document {
        return Document("\$gt", listOf(field, value))
    }

    fun gte(field: String, value: Any): Document {
        return Document("\$gte", listOf(field, value))
    }

    fun lt(field: String, value: Any): Document {
        return Document("\$lt", listOf(field, value))
    }

    fun lte(field: String, value: Any): Document {
        return Document("\$lte", listOf(field, value))
    }

    fun and(vararg conditions: Document): Document {
        return Document("\$and", conditions.toList())
    }

    fun or(vararg conditions: Document): Document {
        return Document("\$or", conditions.toList())
    }

    internal fun build(): Document = doc
}