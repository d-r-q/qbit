package qbit.serialization

import kotlinx.serialization.Serializable
import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid

@Serializable
class NodeData(val trxes: Array<out Eav>)

@Serializable
sealed class Node {
        abstract val hash: Hash
}

class NodeRef(override val hash: Hash) : Node()

@Serializable
sealed class NodeVal : Node() {
        abstract val source: DbUuid
        abstract val timestamp: Long
        abstract val data: NodeData
}

@Serializable
class Root(
        override val hash: Hash,
        override val source: DbUuid,
        override val timestamp: Long,
        override val data: NodeData
) : NodeVal()

@Serializable
class Leaf(
        override val hash: Hash,
        val parent: Node,
        override val source: DbUuid,
        override val timestamp: Long,
        override val data: NodeData
) : NodeVal()

@Serializable
class Merge(
        override val hash: Hash,
        val parent1: Node,
        val parent2: Node,
        override val source: DbUuid,
        override val timestamp: Long,
        override val data: NodeData
) :
        NodeVal()

