package com.example.pdfsearch

data class SearchResult(
    val pageIndex: Int,      // 0-based, matches PDFView's page index
    val snippet: String      // small piece of surrounding text for context
)
