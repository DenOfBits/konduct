package io.github.denofbits.konduct.core

import org.bson.Document

class CustomStageBuilder {
    private val stageDoc = Document()
    
    infix fun String.from(value: String) {
        stageDoc[this] = value
    }
    
    infix fun String.from(value: Number) {
        stageDoc[this] = value
    }
    
    infix fun String.from(value: Boolean) {
        stageDoc[this] = value
    }
    
    infix fun String.from(value: Document) {
        stageDoc[this] = value
    }
    
    infix fun String.from(value: Map<String, Any>) {
        stageDoc[this] = Document(value)
    }
    
    infix fun String.from(value: List<Any>) {
        stageDoc[this] = value
    }
    
    infix fun String.from(block: DocBuilder.() -> Unit) {
        val builder = DocBuilder()
        builder.block()
        stageDoc[this] = builder.build()
    }
    
    internal fun build(): Document = stageDoc
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
    
    fun list(vararg items: Any): List<Any> = items.toList()
    
    fun doc(block: DocBuilder.() -> Unit): Document {
        val builder = DocBuilder()
        builder.block()
        return builder.build()
    }
    
    internal fun build(): Document = doc
}