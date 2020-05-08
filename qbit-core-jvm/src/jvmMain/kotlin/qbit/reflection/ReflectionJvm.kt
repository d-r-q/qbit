package qbit.reflection

import qbit.api.QBitException
import qbit.api.gid.Gid
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

actual fun findProperties(type: KClass<*>): List<KCallable<*>> {
    val copys = type.members
        .filterIsInstance<KFunction<*>>()
        .filter { it.name == "copy" && it.returnType.classifier == type }

    val constrs = type.constructors

    val copy = copys.sortedBy { it.parameters.size }
        .firstOrNull {
            constrs
                .filter { c ->
                    // filter `this`
                    val explicitParams = it.parameters.filter { it.name != null }

                    val paramPairs = c.parameters.zip(explicitParams)
                    val allMatches = paramPairs.all {
                        it.first.name == it.second.name && it.first.type.classifier == it.second.type.classifier
                    }
                    c.parameters.size == explicitParams.size && allMatches
                }
                .any()
        }

    copy ?: throw QBitException("Could not determine properties for type $type. Is it data class?")

    val dataClassProps = copy.parameters.map { it.name }.toSet()
    return type.members
        .filterIsInstance<KProperty1<*, *>>()
        .filter { it.name in dataClassProps }
}

actual fun <T : Any> findPrimaryConstructor(type: KClass<T>): KFunction<T> {

    return type.constructors.filter {
            val props = findProperties(type).map { p -> p.name }.toSet()
            it.parameters.all { p -> p.name in props }
        }
        .maxBy { it.parameters.size }
        ?: throw QBitException("There is no primary constructor for type $type")
}

@Suppress("UNCHECKED_CAST")
actual fun <T : Any> default(type: KClass<T>): T =
    defaults.getOrPut(type) {
        when (type) {
            Boolean::class -> false
            String::class -> ""
            Byte::class -> 0.toByte()
            Int::class -> 0
            Long::class -> 0L
            List::class -> listOf<Any>()
            ByteArray::class -> ByteArray(0)
            else -> {
                val constr = type.constructors
                    // a temporary workaround to make reflection factoring work with serializable entities
                    .filter { it.parameters.none { it.name == "serializationConstructorMarker" } }
                    .first()
                val args = constr.parameters.map {
                        if (it.type.isMarkedNullable) {
                            it to null
                        } else {
                            it to default(it.type.classifier as KClass<*>)
                        }
                    }
                    .toMap()
                constr.callBy(args)
            }
        }
    } as T


