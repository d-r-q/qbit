package qbit.serialization

import qbit.api.model.Hash


interface NodesStorage {

    suspend fun store(n: NodeVal<Hash?>): NodeVal<Hash>

    fun hasNode(head: Node<Hash>): Boolean

    fun load(n: NodeRef): NodeVal<Hash>?

}