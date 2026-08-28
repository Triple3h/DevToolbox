package com.tripleh.devtoolbox.annotations.extraction

import com.intellij.psi.PsiArrayInitializerMemberValue
import com.tripleh.devtoolbox.annotations.extraction.PsiExpressionExtractor.extractExpression
import com.tripleh.devtoolbox.utils.unquote

class PsiArrayInitializerMemberValueExtractor : PsiAnnotationValueExtractor<PsiArrayInitializerMemberValue> {

    override fun extract(value: PsiArrayInitializerMemberValue): List<String> = value.initializers.map {
        val element = extractExpression(it)
        when {
            element.isNotBlank() -> element
            it.text.isNotBlank() -> it.text.unquote()
            else -> ""
        }
    }
}
