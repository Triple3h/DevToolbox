package com.tripleh.devtoolbox.annotations.extraction

import com.intellij.psi.PsiPolyadicExpression
import com.tripleh.devtoolbox.annotations.extraction.PsiExpressionExtractor.extractExpression

class PsiPolyadicExpressionExtractor : PsiAnnotationValueExtractor<PsiPolyadicExpression> {

    override fun extract(value: PsiPolyadicExpression) = listOf(extractExpression(value))
}
