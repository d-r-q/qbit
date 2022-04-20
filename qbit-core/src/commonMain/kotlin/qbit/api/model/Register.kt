package qbit.api.model

import kotlinx.serialization.Serializable

@Serializable
class Register<T>(
    private var entries: List<T>
) {
    fun getValues(): List<T> = entries
    
    fun setValue(t: T) {
        entries = listOf(t)
    }
}