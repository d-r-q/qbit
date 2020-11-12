package qbit.serialization

import qbit.api.model.Eav
import qbit.api.model.Hash
import qbit.api.system.DbUuid

class NodeData(val trxes: Array<out Eav>)

sealed class Node<out H : Hash?>(val parentHash: H)

class NodeRef(parentHash: Hash) : Node<Hash>(parentHash)

sealed class NodeVal<out H : Hash?>(parentHash: H, val source: DbUuid, val timestamp: Long, val data: NodeData) : Node<H>(parentHash)

class Root<out H : Hash?>(parentHash: H, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(parentHash, source, timestamp, data)

class Leaf<out H : Hash?>(parentHash: H, val parent: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(parentHash, source, timestamp, data)

class Merge<out H : Hash?>(parentHash: H, val parent1: Node<Hash>, val parent2: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) :
        NodeVal<H>(parentHash, source, timestamp, data)

