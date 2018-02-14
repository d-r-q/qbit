package qbit

import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.schema.Attr
import qbit.serialization.SimpleSerialization

class DbTest {

    @Test
    fun testCreateIndex() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = System.currentTimeMillis()
        val eid = EID(0, 0)

        val _attr1 = Attr(root["attr1"], QInt)
        val _attr2 = Attr(root["attr2"], QInt)
        val _attr3 = Attr(root["attr3"], QInt)

        val n1 = Root(null, dbUuid, time1, NodeData(arrayOf(Fact(eid, _attr1, 0))))
        val n2 = Leaf(nullHash, toHashed(n1), dbUuid, time1 + 1, NodeData(arrayOf(
                Fact(eid, _attr1, 1),
                Fact(eid, _attr2, 0))))
        val n3 = Leaf(nullHash, toHashed(n2), dbUuid, time1 + 2, NodeData(arrayOf(
                Fact(eid, _attr1, 2),
                Fact(eid, _attr2, 1),
                Fact(eid, _attr3, 0))))

        val index = Index(Graph({ _ -> null }), n3)
        assertEquals(0, index.entitiesByAttrVal("/attr1", 0).size)
        assertEquals(0, index.entitiesByAttrVal("/attr1", 1).size)
        assertEquals(0, index.entitiesByAttrVal("/attr2", 0).size)
        assertEquals(3, index.eavt.size)
        assertEquals(2, index.entityById(eid)!!["/attr1"]!!)
        assertEquals(1, index.entityById(eid)!!["/attr2"]!!)
        assertEquals(0, index.entityById(eid)!!["/attr3"]!!)
    }

    private fun toHashed(n: NodeVal<Hash?>): Node<Hash> {
        val data = SimpleSerialization.serializeNode(n)
        val hash = hash(data)
        return when (n) {
            is Root -> Root(hash, n.source, n.timestamp, n.data)
            is Leaf -> Leaf(hash, n.parent, n.source, n.timestamp, n.data)
            else -> throw IllegalArgumentException("Unexpected $n")
        }
    }

}