package io.github.denofbits.konduct.builders

import io.github.denofbits.konduct.core.CustomAggregationOperation
import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class LookupAndMergeBuilder<T : Any, F : Any>(private val fromClass: KClass<F>) {
    private var fromCollection: String? = null
    private var localField: String? = null
    private var foreignField: String? = null
    private val mergeFields = mutableListOf<String>()
    
    fun from() {
        fromCollection = fromClass.simpleName?.lowercase() + "s"
    }
    
    fun from(collectionName: String) {
        fromCollection = collectionName
    }
    
    fun on(local: KProperty1<T, *>, foreign: KProperty1<F, *>) {
        localField = local.getFieldName()
        foreignField = foreign.getFieldName()
    }
    
    fun merge(vararg fields: KProperty1<F, *>) {
        mergeFields.addAll(fields.map { it.getFieldName() })
    }
    
    fun mergeFields(vararg fieldNames: String) {
        mergeFields.addAll(fieldNames)
    }
    
    internal fun build(): List<AggregationOperation> {
        val fromColl = fromCollection ?: throw IllegalStateException("from() not specified")
        val localF = localField ?: throw IllegalStateException("on() not specified")
        val foreignF = foreignField ?: throw IllegalStateException("on() not specified")
        
        if (mergeFields.isEmpty()) {
            throw IllegalStateException("merge() or mergeFields() not specified")
        }
        
        val stages = mutableListOf<AggregationOperation>()
        
        // 1. Lookup stage
        val lookupDoc = Document()
        lookupDoc["from"] = fromColl
        lookupDoc["localField"] = localF
        lookupDoc["foreignField"] = foreignF
        lookupDoc["as"] = "_lookupTemp"
        
        stages.add(CustomAggregationOperation(Document("\$lookup", lookupDoc)))
        
        // 2. Unwind (preserve null)
        val unwindDoc = Document("\$unwind", Document()
            .append("path", "\$_lookupTemp")
            .append("preserveNullAndEmptyArrays", true)
        )
        stages.add(CustomAggregationOperation(unwindDoc))
        
        // 3. AddFields to merge
        val addFieldsDoc = Document()
        mergeFields.forEach { field ->
            addFieldsDoc[field] = "\$_lookupTemp.$field"
        }
        stages.add(CustomAggregationOperation(Document("\$addFields", addFieldsDoc)))
        
        // 4. Remove temp field
        val projectDoc = Document("_lookupTemp", 0)
        stages.add(CustomAggregationOperation(Document("\$project", projectDoc)))
        
        return stages
    }
}