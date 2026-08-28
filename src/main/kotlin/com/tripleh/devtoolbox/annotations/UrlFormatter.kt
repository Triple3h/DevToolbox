package com.tripleh.devtoolbox.annotations

interface UrlFormatter {

    fun format(classMapping: String, methodMapping: String, param: String = ""): String
}
