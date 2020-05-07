package qbit.factoring

import qbit.api.gid.Gid
import qbit.api.model.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1


val collectionTypes = setOf(List::class)

val types: Map<KClass<*>, DataType<*>> = mapOf(
        Boolean::class to QBoolean,
        Byte::class to QByte,
        Int::class to QInt,
        Long::class to QLong,
        String::class to QString,
        ByteArray::class to QBytes,
        Gid::class to QGid,
        Any::class to QRef
)

val valueTypes = types.values
        .filter { it.value() }
        .map { it.typeClass() }

internal fun findGidProp(root: Any): KCallable<*> =
        root::class.members
                .firstOrNull {
                    it is KProperty1<*, *> && it.name == "id" && (it.returnType.classifier == Long::class || it.returnType.classifier == Gid::class)
                }
                ?: throw IllegalArgumentException("Entity $root does not contains `id: (Long|Gid)` property")

fun KClass<*>.attrName(prop: KProperty1<*, *>): String =
        "." + this.qualifiedName!! + "/" + prop.name

fun KClass<*>.attrName(prop: KProperty0<*>): String =
        "." + this.qualifiedName!! + "/" + prop.name

