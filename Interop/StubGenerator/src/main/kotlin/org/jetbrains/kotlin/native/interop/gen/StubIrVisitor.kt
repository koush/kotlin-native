/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

interface StubIrVisitor<T, R> {

    fun visitClass(element: ClassStub, data: T): R

    fun visitTypealias(element: TypealiasStub, data: T): R

    fun visitFunction(element: FunctionStub, data: T): R

    fun visitProperty(element: PropertyStub, data: T): R

    fun visitConstructor(constructorStub: ConstructorStub, data: T): R

    fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: T): R

    fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: T): R
}

abstract class SimpleStubIrVisitor<T, R> : StubIrVisitor<T, R> {

    private fun defaultBehaviour(element: StubIrElement): Nothing =
            error("Unsupported stub element: $element")

    override fun visitClass(element: ClassStub, data: T): R =
            defaultBehaviour(element)

    override fun visitTypealias(element: TypealiasStub, data: T): R =
            defaultBehaviour(element)

    override fun visitFunction(element: FunctionStub, data: T): R =
            defaultBehaviour(element)

    override fun visitProperty(element: PropertyStub, data: T): R =
            defaultBehaviour(element)

    override fun visitConstructor(constructorStub: ConstructorStub, data: T): R =
            defaultBehaviour(constructorStub)

    override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: T): R =
            defaultBehaviour(propertyAccessor)

    override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: T): R =
            defaultBehaviour(simpleStubContainer)
}