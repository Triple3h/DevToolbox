package com.tripleh.devtoolbox.annotations.jaxrs

import com.tripleh.devtoolbox.annotations.UrlFormatter
import com.tripleh.devtoolbox.utils.dropFirstEmptyStringIfExists

object JaxRsUrlFormatter : UrlFormatter {

    override fun format(classMapping: String, methodMapping: String, param: String): String {
        val classPathSeq = classMapping.splitToSequence('/').filterNot { it.isBlank() }
        val methodPathList = methodMapping.split('/').dropFirstEmptyStringIfExists()
        return (classPathSeq + methodPathList).joinToString(separator = "/", prefix = "/")
    }
}
