package ru.otus.otuskotlin.catalogue.transport.common.models

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDeleteQuery(
        var categoryId:String? = null,
        var debug:Debug? = null
) {
    @Serializable
    class Debug
}