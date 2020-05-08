package qbit.typing

import kotlinx.serialization.*
import qbit.api.QBitException
import qbit.api.gid.NullGid
import qbit.api.model.*
import qbit.factoring.serializatoin.AttrName


fun readSchema(rootDesc: SerialDescriptor): List<Attr<Any>> {
    return rootDesc.elementDescriptors().withIndex().map { (idx, desc) ->
        val dataType = DataType.of(desc)
        Attr<Any>(NullGid, AttrName(rootDesc, idx).asString(), dataType.code, false, dataType.isList())
    }
}

private fun DataType.Companion.of(desc: SerialDescriptor): DataType<*> =
    when (desc.kind) {
        StructureKind.CLASS -> QRef
        StructureKind.LIST -> DataType.of(desc.getElementDescriptor(0))
        PrimitiveKind.STRING -> QString
        PrimitiveKind.BOOLEAN -> QBoolean
        PrimitiveKind.BYTE -> QByte
        PrimitiveKind.INT -> QInt
        PrimitiveKind.LONG -> QLong
        else -> throw QBitException("Unsupported SerialKind ${desc.kind}")
    }
