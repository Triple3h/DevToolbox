package com.tripleh.devtoolbox.utils

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtilCore

fun PsiAnnotation.isMethodAnnotation() = fetchAnnotatedPsiElement(this) is PsiMethod

fun PsiAnnotation.fetchAnnotatedMethod() = fetchAnnotatedPsiElement(this) as PsiMethod

private tailrec fun fetchAnnotatedPsiElement(psiElement: PsiElement): PsiElement {
    val parent: PsiElement = psiElement.parent ?: return PsiUtilCore.NULL_PSI_ELEMENT
    if (parent is PsiMethod || parent is PsiClass) return parent
    return fetchAnnotatedPsiElement(parent)
}

private val SUMMARY_ANNOTATION_NAMES = setOf("Operation", "ApiOperation")

/**
 * Best-effort extraction of a human readable endpoint summary from documentation
 * annotations on a method: @Operation(summary = …) or @ApiOperation(value = …).
 * Only string literals are read; anything more complex yields no summary.
 */
fun PsiMethod.findApiSummary(): String {
    for (annotation in annotations) {
        if (!annotation.hasSimpleNames(SUMMARY_ANNOTATION_NAMES)) continue
        for (attribute in arrayOf("summary", "value")) {
            val value = annotation.findDeclaredAttributeValue(attribute)?.let { it as? PsiLiteralExpression }?.value
            if (value is String && value.isNotBlank()) return value
        }
    }
    return ""
}

private fun PsiAnnotation.hasSimpleNames(names: Set<String>): Boolean {
    val qualified = qualifiedName
    if (qualified != null) {
        return qualified.substringAfterLast('.') in names
    }
    val simple = text.substringBefore('(').removePrefix("@").substringAfterLast('.')
    return simple in names
}
