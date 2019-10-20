package qbit.serialization

import qbit.Hash
import qbit.model.Fact
import qbit.db.DbUuid

class NodeData(val trx: Array<out Fact>)

sealed class Node<out H : Hash?>(val hash: H)

class NodeRef(hash: Hash) : Node<Hash>(hash)

sealed class NodeVal<out H : Hash?>(hash: H, val source: DbUuid, val timestamp: Long, val data: NodeData) : Node<H>(hash)

class Root<out H : Hash?>(hash: H, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(hash, source, timestamp, data)

class Leaf<out H : Hash?>(hash: H, val parent: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) : NodeVal<H>(hash, source, timestamp, data)

class Merge<out H : Hash?>(hash: H, val parent1: Node<Hash>, val parent2: Node<Hash>, source: DbUuid, timestamp: Long, data: NodeData) :
        NodeVal<H>(hash, source, timestamp, data)

