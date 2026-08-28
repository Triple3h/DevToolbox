package com.tripleh.devtoolbox.annotations

import com.tripleh.devtoolbox.RequestMappingItem

object UnknownAnnotation : MappingAnnotation {
    private val mappingItems = emptyList<RequestMappingItem>()
    override fun values(): List<RequestMappingItem> = mappingItems
}
