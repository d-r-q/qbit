package qbit.factoring

import kotlinx.serialization.*


abstract class StubEncoder : Encoder, CompositeEncoder {

    override fun beginStructure(
        descriptor: SerialDescriptor,
        vararg typeSerializers: KSerializer<*>
    ): CompositeEncoder {
        return this
    }

    override fun encodeBoolean(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun encodeByte(value: Byte) {
        TODO("Not yet implemented")
    }

    override fun encodeChar(value: Char) {
        TODO("Not yet implemented")
    }

    override fun encodeDouble(value: Double) {
        TODO("Not yet implemented")
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeFloat(value: Float) {
        TODO("Not yet implemented")
    }

    override fun encodeInt(value: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeLong(value: Long) {
        TODO("Not yet implemented")
    }

    override fun encodeNull() {
        TODO("Not yet implemented")
    }

    override fun encodeShort(value: Short) {
        TODO("Not yet implemented")
    }

    override fun encodeString(value: String) {
        TODO("Not yet implemented")
    }

    override fun encodeUnit() {
        TODO("Not yet implemented")
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        TODO("Not yet implemented")
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        TODO("Not yet implemented")
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        TODO("Not yet implemented")
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        TODO("Not yet implemented")
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        TODO("Not yet implemented")
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null) {
             when (descriptor.getElementDescriptor(index).kind) {
                 PrimitiveKind.BOOLEAN -> encodeBooleanElement(descriptor, index, value as Boolean)
                 PrimitiveKind.BYTE -> encodeByteElement(descriptor, index, value as Byte)
                 PrimitiveKind.CHAR -> encodeCharElement(descriptor, index, value as Char)
                 PrimitiveKind.SHORT -> encodeShortElement(descriptor, index, value as Short)
                 PrimitiveKind.INT -> encodeIntElement(descriptor, index, value as Int)
                 PrimitiveKind.LONG -> encodeLongElement(descriptor, index, value as Long)
                 PrimitiveKind.FLOAT -> encodeFloatElement(descriptor, index, value as Float)
                 PrimitiveKind.DOUBLE -> encodeDoubleElement(descriptor, index, value as Double)
                 PrimitiveKind.STRING -> encodeStringElement(descriptor, index, value as String)
                 else -> encodeSerializableElement(descriptor, index, serializer, value)
             }
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        TODO("Not yet implemented")
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        TODO("Not yet implemented")
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        TODO("Not yet implemented")
    }

    override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

}