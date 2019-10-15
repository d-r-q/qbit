package qbit

import qbit.Users.country
import qbit.Users.extId
import qbit.Users.name
import qbit.Users.nicks
import qbit.Users.reviewer
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
        assertArrayEquals(arrayOf(Gid(pChen.id!!)), db.query(attrIn(extId, 1, 3), attrIs(name, "Peter Chen")).map { it.gid }.toList().toTypedArray())
    }

    @Test
    fun `Entity with multiple values of list attribute should be returned from query only once`() {
        val dbUuid = DbUuid(IID(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts()).toTypedArray()))
        val index = Index(Graph { null }, root)

        val db = IndexDb(index)
        assertArrayEquals(arrayOf(eCodd.gid), db.query(attrIn(nicks, "n", "u")).map { it.gid }.toList().toTypedArray())
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

    @Ignore
    @Test
    fun testUniqueList() {
        val uniqueList = Attr<Int>(null, "uniqueList", QInt.code, true, true)
        assertNull(uniqueList)
        // todo: what is expected behaviour for such attributes?
    }

    @Test
    fun `pull with fetch = Eager should fetch nullable refs`() {
        val dbUuid = DbUuid(IID(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + reviewer.toFacts() + country.toFacts() +
                Countries.name.toFacts() + Countries.population.toFacts() +
                eCodd.copy(reviewer = pChen).toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val graph = Graph { nodes[it.hash] }
        val db = IndexDb(Index(graph, root))
        val pc = db.pull(eCodd.gid, User::class, Eager)!!
        assertNotNull(pc.reviewer)
    }

}