/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.utils.addIfNotNull

class StubIrMetadataEmitter(
        private val stubIrBuilderResult: StubIrBuilderResult
) {
    fun emit(): KmPackage = stubIrBuilderResult.stubs.accept(packageProducer, null)

    private val packageProducer = object : SimpleStubIrVisitor<Nothing?, KmPackage>() {
        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: Nothing?): KmPackage {
            return KmPackage().apply {
                typeAliases += simpleStubContainer.typealiases.map { it.accept(mapper, simpleStubContainer) as KmTypeAlias }
                functions += simpleStubContainer.functions.map { it.accept(mapper, simpleStubContainer) as KmFunction }
                properties += simpleStubContainer.properties.map { it.accept(mapper, simpleStubContainer) as KmProperty }
            }
        }
    }

    private val mapper = object : StubIrVisitor<StubContainer?, Any> {
        override fun visitClass(element: ClassStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitTypealias(element: TypealiasStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitFunction(element: FunctionStub, data: StubContainer?): Any =
                KmFunction(element.flags, element.name).also { km ->
                    km.returnType = element.returnType.map()
                    km.valueParameters += element.parameters.map { it.map() }
                    km.typeParameters += element.typeParameters.map { it.map() }
                    km.annotations += element.annotations.map { it.map() }
                }

        override fun visitProperty(element: PropertyStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitConstructor(constructorStub: ConstructorStub, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: StubContainer?): Any {
            TODO("not implemented")
        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: StubContainer?): Any {
            TODO("not implemented")
        }
    }

    private fun StubType.map(): KmType = when (this) {
        is ClassifierStubType -> KmType(flags).also { km ->
            km.arguments += typeArguments.map { it.map() }
            if (isTypealias) {
                km.abbreviatedType = abbreviatedType
                km.classifier = expandedType!!.map().classifier
            } else {
                km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
            }

        }
        is FunctionalType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is TypeParameterType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeParameter(id)
        }
    }

    private fun FunctionParameterStub.map(): KmValueParameter =
            KmValueParameter(flags, name).also { km ->
                type.map().let {
                    if (isVararg) {
                        km.varargElementType = it
                    } else {
                        km.type = it
                    }
                }
            }

    private fun TypeParameterStub.map(): KmTypeParameter =
            KmTypeParameter(flagsOf(), name, id, KmVariance.INVARIANT).also { km ->
                km.upperBounds.addIfNotNull(upperBound?.map())
            }

    private fun TypeArgument.map(): KmTypeProjection = when (this) {
        TypeArgument.StarProjection -> KmTypeProjection.STAR
        is TypeArgumentStub -> KmTypeProjection(variance.map(), type.map())
        else -> error("Unexpected TypeArgument: $this")
    }

    private fun TypeArgument.Variance.map(): KmVariance = when (this) {
        TypeArgument.Variance.INVARIANT -> KmVariance.INVARIANT
        TypeArgument.Variance.IN -> KmVariance.IN
        TypeArgument.Variance.OUT -> KmVariance.OUT
    }

    private fun AnnotationStub.map(): KmAnnotation = when (this) {
        AnnotationStub.ObjC.ConsumesReceiver -> TODO()
        AnnotationStub.ObjC.ReturnsRetained -> TODO()
        is AnnotationStub.ObjC.Method -> TODO()
        is AnnotationStub.ObjC.Factory -> TODO()
        AnnotationStub.ObjC.Consumed -> TODO()
        is AnnotationStub.ObjC.Constructor -> TODO()
        is AnnotationStub.ObjC.ExternalClass -> TODO()
        AnnotationStub.CCall.CString -> TODO()
        AnnotationStub.CCall.WCString -> TODO()
        is AnnotationStub.CCall.Symbol -> KmAnnotation(
                "kotlinx/cinterop/internal/CCall",
                mapOf("id" to KmAnnotationArgument.StringValue(symbolName))
        )
        is AnnotationStub.CStruct -> TODO()
        is AnnotationStub.CNaturalStruct -> TODO()
        is AnnotationStub.CLength -> TODO()
        is AnnotationStub.Deprecated -> TODO()
    }

    private val FunctionStub.flags: Flags
        get() = arrayOf(
                Flag.Common.IS_PUBLIC,
                Flag.Function.IS_EXTERNAL,
                Flag.HAS_ANNOTATIONS
        ).let { flagsOf(*it) }

    private val StubType.flags: Flags
        get() = listOfNotNull(
                if (nullable) Flag.Type.IS_NULLABLE else null
        ).let { flagsOf(*it.toTypedArray()) }

    private val FunctionParameterStub.flags: Flags
        get() = flagsOf()

    private val TypeParameterType.id: Int
        get() = TODO()

    private val TypeParameterStub.id: Int
        get() = TODO()

    private val ClassifierStubType.abbreviatedType: KmType
        get() = KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeAlias(classifier.fqNameSerialized)
        }
}

private val Classifier.fqNameSerialized: String
        get() = fqName.replace('.', '/')