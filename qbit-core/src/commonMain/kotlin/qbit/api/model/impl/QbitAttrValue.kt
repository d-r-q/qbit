package qbit.api.model.impl

import qbit.api.model.Attr
import qbit.api.model.AttrValue

internal class QbitAttrValue<T : Any>(override val attr: Attr<T>, override val value: T) :
    AttrValue<Attr<T>, T> {
    override fun toString(): String = "${attr.name}=$value"
}