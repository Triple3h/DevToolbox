package com.tripleh.devtoolbox.annotations.micronaut

import com.tripleh.devtoolbox.annotations.UrlFormatter
import com.tripleh.devtoolbox.utils.dropFirstEmptyStringIfExists

object MicronautUrlFormatter : UrlFormatter {

    override fun format(classMapping: String, methodMapping: String, param: String): String {
        val classPathSeq = classMapping.splitToSequence('/').filterNot { it.isBlank() }
        val methodPathList = methodMapping.split('/').dropFirstEmptyStringIfExists()
        return (classPathSeq + methodPathList).joinToString(separator = "/", prefix = "/")
    }
}
