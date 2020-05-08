package qbit

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import qbit.api.Attrs
import qbit.api.Instances
import qbit.api.model.Attr
import qbit.api.model.impl.QTombstone
import qbit.api.system.Instance
import qbit.api.tombstone


val qbitSerialModule = SerializersModule {
    contextual(Attr::class, Attr.serializer(FakeSerializer<Any>()))
    contextual(Instance::class, Instance.serializer())
    contextual(QTombstone::class, QTombstone.serializer())
}

val bootstrapSchema: Map<String, Attr<Any>> = mapOf(
    (Attrs.name.name to Attrs.name) as Pair<String, Attr<Any>>,
    (Attrs.type.name to Attrs.type) as Pair<String, Attr<Any>>,
    (Attrs.unique.name to Attrs.unique) as Pair<String, Attr<Any>>,
    (Attrs.list.name to Attrs.list) as Pair<String, Attr<Any>>,
    (Instances.forks.name to Instances.forks) as Pair<String, Attr<Any>>,
    (Instances.nextEid.name to Instances.nextEid) as Pair<String, Attr<Any>>,
    (Instances.iid.name to Instances.iid) as Pair<String, Attr<Any>>,
    (".qbit.api/tombstone" to tombstone) as Pair<String, Attr<Any>>
)

private class FakeSerializer<T> : KSerializer<T> {

    override val descriptor: SerialDescriptor
        get() = TODO("not implemented")

    override fun deserialize(decoder: Decoder): T {
        TODO("not implemented")
    }

    override fun serialize(encoder: Encoder, value: T) {
        TODO("not implemented")
    }

}