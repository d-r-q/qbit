package qbit.platform

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty

expect fun getRawType(obj: KProperty<*>): KClass<*>

expect fun getRawType(obj: KParameter): KClass<*>

expect fun getRawTypeOfActualTypeArgument(obj: KProperty<*>): KClass<*>