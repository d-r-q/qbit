package qbit.factorization

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextual
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.gid.Gid
import qbit.api.model.Attr
import qbit.collections.IdentityMap
import kotlin.reflect.KClass

val serializers = HashMap<KClass<*>, KSerializer<*>>()

fun ksDestruct(e: Any, schema: (String) -> Attr<*>?, gids: Iterator<Gid>): EntityGraphFactorization {
    val sm = serializersModuleOf(serializers)
    val encoder = EntityEncoder(sm)
    sm.getContextual(e)!!.serialize(encoder, e)
    return EntityGraphFactorization(IdentityMap())
}

class EntityEncoder(override val context: SerialModule) : Encoder {

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeBoolean(value: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeByte(value: Byte) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeChar(value: Char) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeDouble(value: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeFloat(value: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeInt(value: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeLong(value: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeNotNullMark() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeNull() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeShort(value: Short) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeString(value: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun encodeUnit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
