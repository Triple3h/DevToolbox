package com.tripleh.devtoolbox.annotations

import com.intellij.psi.PsiAnnotation
import com.tripleh.devtoolbox.RequestMappingItem
import com.tripleh.devtoolbox.annotations.jaxrs.DELETE
import com.tripleh.devtoolbox.annotations.jaxrs.GET
import com.tripleh.devtoolbox.annotations.jaxrs.HEAD
import com.tripleh.devtoolbox.annotations.jaxrs.OPTIONS
import com.tripleh.devtoolbox.annotations.jaxrs.PATCH
import com.tripleh.devtoolbox.annotations.jaxrs.POST
import com.tripleh.devtoolbox.annotations.jaxrs.PUT
import com.tripleh.devtoolbox.annotations.micronaut.Delete
import com.tripleh.devtoolbox.annotations.micronaut.Get
import com.tripleh.devtoolbox.annotations.micronaut.Head
import com.tripleh.devtoolbox.annotations.micronaut.Options
import com.tripleh.devtoolbox.annotations.micronaut.Patch
import com.tripleh.devtoolbox.annotations.micronaut.Post
import com.tripleh.devtoolbox.annotations.micronaut.Put
import com.tripleh.devtoolbox.annotations.spring.DeleteMapping
import com.tripleh.devtoolbox.annotations.spring.GetMapping
import com.tripleh.devtoolbox.annotations.spring.PatchMapping
import com.tripleh.devtoolbox.annotations.spring.PostMapping
import com.tripleh.devtoolbox.annotations.spring.PutMapping
import com.tripleh.devtoolbox.annotations.spring.RequestMapping

interface MappingAnnotation {

    fun values(): List<RequestMappingItem>

    companion object {
        val supportedAnnotations = listOf(
            RequestMapping::class.java.simpleName,
            GetMapping::class.java.simpleName,
            PostMapping::class.java.simpleName,
            PutMapping::class.java.simpleName,
            PatchMapping::class.java.simpleName,
            DeleteMapping::class.java.simpleName,

            GET::class.java.simpleName,
            PUT::class.java.simpleName,
            POST::class.java.simpleName,
            OPTIONS::class.java.simpleName,
            HEAD::class.java.simpleName,
            DELETE::class.java.simpleName,
            PATCH::class.java.simpleName,

            Delete::class.java.simpleName,
            Get::class.java.simpleName,
            Head::class.java.simpleName,
            Options::class.java.simpleName,
            Patch::class.java.simpleName,
            Post::class.java.simpleName,
            Put::class.java.simpleName
        )

        fun mappingAnnotation(annotationName: String, psiAnnotation: PsiAnnotation): MappingAnnotation {
            return when (annotationName) {
                RequestMapping::class.java.simpleName -> RequestMapping(psiAnnotation)
                GetMapping::class.java.simpleName -> GetMapping(psiAnnotation)
                PostMapping::class.java.simpleName -> PostMapping(psiAnnotation)
                PutMapping::class.java.simpleName -> PutMapping(psiAnnotation)
                PatchMapping::class.java.simpleName -> PatchMapping(psiAnnotation)
                DeleteMapping::class.java.simpleName -> DeleteMapping(psiAnnotation)

                GET::class.java.simpleName -> GET(psiAnnotation)
                PUT::class.java.simpleName -> PUT(psiAnnotation)
                POST::class.java.simpleName -> POST(psiAnnotation)
                OPTIONS::class.java.simpleName -> OPTIONS(psiAnnotation)
                HEAD::class.java.simpleName -> HEAD(psiAnnotation)
                DELETE::class.java.simpleName -> DELETE(psiAnnotation)
                PATCH::class.java.simpleName -> PATCH(psiAnnotation)

                Get::class.java.simpleName -> Get(psiAnnotation)
                Put::class.java.simpleName -> Put(psiAnnotation)
                Post::class.java.simpleName -> Post(psiAnnotation)
                Options::class.java.simpleName -> Options(psiAnnotation)
                Head::class.java.simpleName -> Head(psiAnnotation)
                Delete::class.java.simpleName -> Delete(psiAnnotation)
                Patch::class.java.simpleName -> Patch(psiAnnotation)

                else -> UnknownAnnotation
            }
        }
    }
}
