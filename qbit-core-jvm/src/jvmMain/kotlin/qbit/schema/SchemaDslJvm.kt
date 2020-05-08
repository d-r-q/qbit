package qbit.schema

import qbit.api.model.Attr
import qbit.api.model.QRef
import qbit.factoring.attrName
import qbit.factoring.collectionTypes
import qbit.factoring.types
import qbit.factoring.valueTypes
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

actual fun schemaFor(type: KClass<*>, unique: Set<String>): Collection<Attr<Any>> {
    val getters = type.members.filterIsInstance<KProperty1<*, *>>()
    val (ids, attrs) = getters.partition { it.name == "id" && it.returnType.classifier == Long::class }
    val id = ids.firstOrNull()
    id ?: throw IllegalArgumentException("Type $type does not contains id: Long property")
    return attrs.map {
        when (it.returnType.classifier) {
            in valueTypes -> Attr(
                null,
                type.attrName(it),
                types.getValue(it.returnType.classifier as KClass<*>).code,
                type.attrName(it) in unique,
                false
            )
            in collectionTypes -> {
                when (val valueType = it.returnType.arguments[0].type!!.classifier as KClass<*>) {
                    in valueTypes -> Attr(
                        null,
                        type.attrName(it),
                        types.getValue(valueType).list().code,
                        type.attrName(it) in unique,
                        true
                    )
                    else -> Attr<Any>(
                        null,
                        type.attrName(it),
                        QRef.list().code,
                        type.attrName(it) in unique,
                        true
                    )
                }
            }
            else -> {
                Attr(
                    null,
                    type.attrName(it),
                    QRef.code,
                    type.attrName(it) in unique,
                    false
                )
            }
        }
    }
}

