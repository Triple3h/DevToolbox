package com.tripleh.devtoolbox.annotations.extraction

import com.intellij.psi.PsiReferenceExpression
import com.tripleh.devtoolbox.annotations.extraction.PsiExpressionExtractor.extractExpression

class PsiReferenceExpressionExtractor : PsiAnnotationValueExtractor<PsiReferenceExpression> {

    override fun extract(value: PsiReferenceExpression): List<String> = listOf(extractExpression(value))
}
