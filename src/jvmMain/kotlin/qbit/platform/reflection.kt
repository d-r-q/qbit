package qbit.platform

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

actual fun getRawType(obj: KProperty<*>): KClass<*> {
    var type = obj.returnType.javaType
    if (type is ParameterizedType) {
        type = type.rawType
    }
    return (type as Class<*>).kotlin
}

actual fun getRawType(obj: KParameter): KClass<*> {
    var type = obj.type.javaType
    if (type is ParameterizedType) {
        type = type.rawType
    }
    return (type as Class<*>).kotlin
}

actual fun getRawTypeOfActualTypeArgument(obj: KProperty<*>): KClass<*> {
    var type = (obj.returnType.javaType as ParameterizedType).actualTypeArguments[0]
    if (type is ParameterizedType) {
        type= type.rawType
    }
    return (type as Class<*>).kotlin
}