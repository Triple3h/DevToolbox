package com.tripleh.devtoolbox.annotations.spring

import com.tripleh.devtoolbox.annotations.UrlFormatter
import com.tripleh.devtoolbox.utils.dropFirstEmptyStringIfExists

object SpringUrlFormatter : UrlFormatter {

    override fun format(classMapping: String, methodMapping: String, param: String): String {
        val classPathSeq = classMapping.splitToSequence('/').filterNot { it.isBlank() }
        val methodPathList = methodMapping.split('/').dropFirstEmptyStringIfExists()
        val path = (classPathSeq + methodPathList).joinToString(separator = "/", prefix = "/").replace("\${", "{")
        return path + if (param.isNotBlank()) " params=$param" else ""
    }
}
