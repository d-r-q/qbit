package qbit

import qbit.Scientists.country
import qbit.Scientists.extId
import qbit.Scientists.name
import qbit.Scientists.nicks
import qbit.Scientists.reviewer
import qbit.factorization.destruct
import qbit.model.gid
import qbit.model.Gid
import qbit.model.Iid
import qbit.model.toFacts
import qbit.platform.currentTimeMillis
import qbit.system.DbUuid
import qbit.index.Indexer
import qbit.query.Eager
import qbit.query.attrIn
import qbit.query.attrIs
import qbit.serialization.*
import qbit.util.Hash
import kotlin.test.Test
import kotlin.test.assertNotNull

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()

        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts() + pChen.toFacts() + mStonebreaker.toFacts() + eBrewer.toFacts()).toTypedArray()))
        val db = Indexer(null, null, identityNodeResolver).index(root)
        assertArrayEquals(arrayOf(Gid(pChen.id!!)), db.query(attrIn(extId, 1, 3), attrIs(name, "Peter Chen")).map { it.gid }.toList().toTypedArray())
    }

    @Test
    fun `Entity with multiple values of list attribute should be returned from query only once`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts()).toTypedArray()))
        val db = Indexer(null, null, identityNodeResolver).index(root)
        assertArrayEquals(arrayOf(eCodd.gid), db.query(attrIn(nicks, "n", "u")).map { it.gid }.toList().toTypedArray())
    }

    @Test
    fun `Indexer can index multiple transactions`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val gids = eBrewer.gid!!.nextGids()
        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                testSchema.flatMap { destruct(it, bootstrapSchema::get, gids) } +
                extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val nodeResolver = mapNodeResolver(nodes)

        var db = Indexer(null, null, nodeResolver).index(root)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData(pChen.toFacts().toList().toTypedArray()))
        nodes[n1.hash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData(mStonebreaker.toFacts().toList().toTypedArray()))
        nodes[n2.hash] = n2

        db = Indexer(db, root.hash, nodeResolver).index(n2)
        assertNotNull(db.pull(eCodd.gid!!))
        assertNotNull(db.pull(pChen.gid!!))
        assertNotNull(db.pull(mStonebreaker.gid!!))
    }

    @Test
    fun `Indexer can index updates`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val nodeResolver = mapNodeResolver(nodes)
        var db = Indexer(null, null, nodeResolver).index(root)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData(pChen.toFacts().toList().toTypedArray()))
        nodes[n1.hash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData(pChen.copy( externalId = 5).toFacts().toList().toTypedArray()))
        nodes[n2.hash] = n2

        db = Indexer(db, root.hash, nodeResolver).index(n2)
        assertNotNull(db.query(attrIs(extId, 5)))
    }

    @Test
    fun `pull with fetch = Eager should fetch nullable refs`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + reviewer.toFacts() + country.toFacts() +
                Countries.name.toFacts() + Countries.population.toFacts() +
                eCodd.copy(reviewer = pChen).toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.hash to root)
        val nodeResolver = mapNodeResolver(nodes)
        val db = Indexer(null, null, nodeResolver).index(root)
        val pc = db.pull(eCodd.gid!!, Scientist::class, Eager)!!
        assertNotNull(pc.reviewer)
    }

}