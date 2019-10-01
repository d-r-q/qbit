package qbit

import qbit.Users.extId
import qbit.Users.name
import qbit.Users.nicks
import qbit.mapping.destruct
import qbit.mapping.gid
import qbit.model.*
import qbit.platform.currentTimeMillis
import qbit.trx.indexTrxLog
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(IID(0, 1))
        val time1 = currentTimeMillis()

        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts() + pChen.toFacts() + mStonebreaker.toFacts() + eBrewer.toFacts()).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index)
        assertArrayEquals(arrayOf(Gid(pChen.id!!)), db.query(attrIn(extId, 1, 3), attrIs(name, "Peter Chen")).map { it.eid }.toList().toTypedArray())
    }

    @Test
    fun `Entity with multiple values of list attribute should be returned from query only once`() {
        val dbUuid = DbUuid(IID(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts()).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index)
        assertArrayEquals(arrayOf(eCodd.gid), db.query(attrIn(nicks, "n", "u")).map { it.eid }.toList().toTypedArray())
    }

    @Test
    fun `Indexer can index multiple transactions`() {
        val dbUuid = DbUuid(IID(0, 1))

        val gids = eBrewer.gid.nextGids()
        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                testSchema.flatMap { destruct(it, bootstrapSchema::get, gids) } +
                extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }
        val index = Index(graph, root)

        var db = IndexDb(index)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData((pChen.toFacts()).toTypedArray()))
        nodes[n1.hash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData((mStonebreaker.toFacts()).toTypedArray()))
        nodes[n2.hash] = n2

        db = indexTrxLog(db, graph, n2, root.hash)
        assertNotNull(db.pull(eCodd.gid))
        assertNotNull(db.pull(pChen.gid))
        assertNotNull(db.pull(mStonebreaker.gid))
    }

    @Test
    fun `Indexer can index updates`() {
        val dbUuid = DbUuid(IID(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }
        val index = Index(graph, root)

        var db = IndexDb(index)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData((pChen.toFacts()).toTypedArray()))
        nodes[n1.hash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData((pChen.copy( externalId = 5).toFacts()).toTypedArray()))
        nodes[n2.hash] = n2

        db = indexTrxLog(db, graph, n2, root.hash)
        assertNotNull(db.query(attrIs(extId, 5)))
    }

    @Test
    fun `StoredEntity returns null, when asked to pull entity via not set attribute`() {
        val dbUuid = DbUuid(IID(0, 1))
        val eids = Gid(0, 0).nextGids()
        val ref = Attr<Any>(eids.next(), "ref")

        val e1 = Entity(eids.next())
        val e2eid = eids.next()
        val e2 = Entity(e2eid, ref eq e1)
        val root = Root(Hash(byteArrayOf(0)), dbUuid, currentTimeMillis(), NodeData((ref.toFacts() + e2.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }

        val indexDb = IndexDb(Index(graph, root))
        val e2Pulled = indexDb.pull(e2eid)!!
        assertNotNull(e2Pulled)
        assertNull(e2Pulled.tryGet(ref))
        // todo: add check, that pull isn't called second time
        assertNull(e2Pulled.tryGet(ref))
    }

    @Ignore
    @Test
    fun testUniqueList() {
        val uniqueList = Attr<Int>(null, "uniqueList", QInt.code, true, true)
        assertNull(uniqueList)
        // todo: what is expected behaviour for such attributes?
    }
}