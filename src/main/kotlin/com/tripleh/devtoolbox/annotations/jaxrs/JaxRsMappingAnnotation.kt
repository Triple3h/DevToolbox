package com.tripleh.devtoolbox.annotations.jaxrs

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceExpression
import com.tripleh.devtoolbox.RequestMappingItem
import com.tripleh.devtoolbox.annotations.MappingAnnotation
import com.tripleh.devtoolbox.annotations.PathAnnotation
import com.tripleh.devtoolbox.annotations.UrlFormatter
import com.tripleh.devtoolbox.annotations.extraction.PsiExpressionExtractor.extractExpression
import com.tripleh.devtoolbox.model.Path
import com.tripleh.devtoolbox.model.PathParameter
import com.tripleh.devtoolbox.utils.fetchAnnotatedMethod

abstract class JaxRsMappingAnnotation(
    private val psiAnnotation: PsiAnnotation,
    private val urlFormatter: UrlFormatter = JaxRsUrlFormatter
) : MappingAnnotation {

    override fun values(): List<RequestMappingItem> =
        fetchRequestMappingItem(psiAnnotation.fetchAnnotatedMethod(), extractMethod())

    abstract fun extractMethod(): String

    private fun fetchRequestMappingItem(psiMethod: PsiMethod, method: String): List<RequestMappingItem> {
        val classMapping = fetchMappingFromClass(psiMethod)
        val methodMapping = fetchMappingFromMethod(psiMethod)
        return listOf(RequestMappingItem(psiMethod, urlFormatter.format(classMapping, methodMapping), method))
    }

    private fun fetchMappingFromClass(psiMethod: PsiMethod): String {
        return psiMethod
            .containingClass
            ?.modifierList
            ?.annotations
            ?.filter { it.qualifiedName == PATH_ANNOTATION }
            ?.flatMap { PathAnnotation(it).fetchMappings(ATTRIBUTE_NAME) }
            ?.firstOrNull() ?: ""
    }

    private fun fetchMappingFromMethod(method: PsiMethod): String {
        val parametersNameWithType = method
            .parameterList
            .parameters
            .mapNotNull { PathParameter(it).extractParameterNameWithType(PATH_PARAM_ANNOTATION, ::extractParameterNameFromAnnotation) }
            .toMap()

        return method
            .modifierList
            .annotations
            .filter { it.qualifiedName == PATH_ANNOTATION }
            .flatMap { PathAnnotation(it).fetchMappings(ATTRIBUTE_NAME) }
            .map { Path(it).addPathVariablesTypes(parametersNameWithType).toFullPath() }
            .firstOrNull() ?: ""
    }

    private fun extractParameterNameFromAnnotation(annotation: PsiAnnotation, defaultValue: String): String {
        return when (val pathVariableValue = annotation.findAttributeValue(ATTRIBUTE_NAME)) {
            is PsiLiteralExpression -> {
                val expression = extractExpression(pathVariableValue)
                expression.ifBlank { defaultValue }
            }
            is PsiReferenceExpression -> {
                val expression = extractExpression(pathVariableValue)
                expression.ifBlank { defaultValue }
            }
            else -> defaultValue
        }
    }

    companion object {
        private const val PATH_ANNOTATION = "javax.ws.rs.Path"
        private const val ATTRIBUTE_NAME = "value"
        private const val PATH_PARAM_ANNOTATION = "javax.ws.rs.PathParam"
    }
}
