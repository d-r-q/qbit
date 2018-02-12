package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.Db.Companion.createIndex
import qbit.serialization.SimpleSerialization

class DbTest {

    @Test
    fun testCreateIndex() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid = EID(0, 0)

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Fact(eid, "attr1", 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Fact(eid, "attr1", 1),
                Fact(eid, "attr2", 0))))
        val n3 = Leaf(nullHash, toHashed(n2), dbUuid, time1 + 2, NodeData(arrayOf(
                Fact(eid, "attr1", 2),
                Fact(eid, "attr2", 1),
                Fact(eid, "attr3", 0))))

        val index = createIndex(Graph({ _ -> null }), n3)
        assertEquals(0, index.entitiesByAttrVal("attr1", 0).size)
        assertEquals(0, index.entitiesByAttrVal("attr1", 1).size)
        assertEquals(0, index.entitiesByAttrVal("attr2", 0).size)
        assertEquals(3, index.eavt.size)
        assertEquals(2, index.entityById(eid)!!["attr1"]!!)
        assertEquals(1, index.entityById(eid)!!["attr2"]!!)
        assertEquals(0, index.entityById(eid)!!["attr3"]!!)
    }

    fun toHashed(n: NodeVal<Hash?>): Node<Hash> {
        val data = SimpleSerialization.serializeNode(n)
        val hash = hash(data)
        return when (n) {
            is Root -> Root(hash, n.source, n.timestamp, n.data)
            is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
            else -> throw IllegalArgumentException("Unexpected $n")
        }
    }

}