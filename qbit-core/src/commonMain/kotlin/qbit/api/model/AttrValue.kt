package qbit.api.model

import qbit.api.model.impl.QbitAttrValue

interface AttrValue<A : Attr<T>, T : Any> {

    val attr: A
    val value: T

    fun toPair() = attr to value

    operator fun component1(): Attr<T> = attr

    operator fun component2(): T = value

}

infix fun <T : Any> Attr<T>.eq(v: T): AttrValue<Attr<T>, T> =
    QbitAttrValue(this, v)
