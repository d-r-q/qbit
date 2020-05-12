package qbit.schema

import kotlinx.serialization.Serializable
import qbit.api.model.DataType
import qbit.api.model.QByte
import qbit.api.model.QBytes
import qbit.api.model.QString
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class SchemaDslTest {

    @JsName("Test_schemaFor_for_entity_with_ByteArray")
    @Test
    fun `Test schemaFor for entity with ByteArray`() {
        // Given class with byte array field

        // When schema for it is generated
        val schema = schemaFor(EntityWithByteArray.serializer().descriptor)

        // Then it contains single attribute
        assertEquals(1, schema.size)
        // and the attibute's type is bytearray
        assertEquals(QBytes.code, schema[0].type, "QBytes expected but got ${DataType.ofCode(schema[0].type)}")
        // and itn't list
        assertFalse(schema[0].list)
        // and itn't unique
        assertFalse(schema[0].unique)
    }

    @JsName("Test_schemaFor_for_entity_with_list_of_ByteArray")
    @Test
    fun `Test schemaFor for entity with list of ByteArrays`() {
        // Given class with list of byte arrays field

        // When schema for it is generated
        val schema = schemaFor(EntityWithListOfByteArray.serializer().descriptor)

        // Then it contains single attribute
        assertEquals(1, schema.size)
        // and the attibute's type is bytearray
        assertEquals(QBytes.list().code, schema[0].type, "QList<QBytes> expected but got ${DataType.ofCode(schema[0].type)}")
        // and itn't list
        assertTrue(schema[0].list)
        // and itn't unique
        assertFalse(schema[0].unique)
    }

    @JsName("Test_schemaFor_for_entity_with_list_of_String")
    @Test
    fun `Test schemaFor for entity with list of String`() {
        // Given class with list of strings field

        // When schema for it is generated
        val schema = schemaFor(EntityWithListOfString.serializer().descriptor)

        // Then it contains single attribute
        assertEquals(1, schema.size)
        // and the attibute's type is bytearray
        assertEquals(QString.list().code, schema[0].type, "QList<QString> expected but got ${DataType.ofCode(schema[0].type)}")
        // and itn't list
        assertTrue(schema[0].list)
        // and itn't unique
        assertFalse(schema[0].unique)
    }

    @JsName("Test_schemaFor_for_entity_with_list_of_bytes")
    @Test
    fun `Test schemaFor for entity with list of bytes`() {
        // Given class with list of strings field

        // When schema for it is generated
        val schema = schemaFor(EntityWithListOfBytes.serializer().descriptor)

        // Then it contains single attribute
        assertEquals(1, schema.size)
        // and the attibute's type is bytearray
        assertEquals(QByte.list().code, schema[0].type, "QList<QByte> expected but got ${DataType.ofCode(schema[0].type)}")
        // and itn't list
        assertTrue(schema[0].list)
        // and itn't unique
        assertFalse(schema[0].unique)
    }

}

@Serializable
data class EntityWithByteArray(val id: Long?, val byteArray: ByteArray)

@Serializable
data class EntityWithListOfByteArray(val id: Long?, val byteArrays: List<ByteArray>)

@Serializable
data class EntityWithListOfString(val id: Long?, val strings: List<String>)

@Serializable
data class EntityWithListOfBytes(val id: Long?, val bytes: List<Byte>)