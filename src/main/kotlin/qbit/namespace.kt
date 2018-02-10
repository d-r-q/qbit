package qbit

import java.util.Arrays.asList

data class Namespace(val name: String, val parent: Namespace? = null) {

    val parts: List<String> =
            if (parent == null) asList(name)
            else parent.parts + name

    operator fun get(key: String) = Key(this, key)

    fun subNs(name: String) = Namespace(name, this)

}

data class Key(val ns: Namespace, val name: String)