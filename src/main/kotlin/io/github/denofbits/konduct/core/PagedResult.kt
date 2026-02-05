package io.github.denofbits.konduct.core

data class PagedResult<T>(
    val data: List<T>,           // The actual page data
    val total: Long,             // Total count of all matching documents
    val page: Int,               // Current page number (1-based)
    val pageSize: Int,           // Number of items per page
    val totalPages: Int          // Total number of pages
)