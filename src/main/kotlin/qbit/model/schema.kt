package qbit.model

class Schema(private val attrs: Map<String, Attr<Any>>) {

    fun find(attrName: String): Attr<Any>? = attrs[attrName]

}
