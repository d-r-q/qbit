package qbit.index

import qbit.*
import qbit.Scientists.country
import qbit.Scientists.extId
import qbit.Scientists.name
import qbit.Scientists.nicks
import qbit.Scientists.reviewer
import qbit.api.db.Eager
import qbit.api.db.attrIn
import qbit.api.db.attrIs
import qbit.api.db.pull
import qbit.api.gid.Gid
import qbit.api.gid.Iid
import qbit.api.gid.nextGids
import qbit.api.model.Hash
import qbit.api.system.DbUuid
import qbit.platform.currentTimeMillis
import qbit.serialization.Leaf
import qbit.serialization.NodeData
import qbit.serialization.NodeVal
import qbit.serialization.Root
import qbit.test.model.Scientist
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertNotNull

class DbTest {

    @Test
    fun testSearchByAttrRangeAndAttrValue() {
        val dbUuid = DbUuid(Iid(0, 1))
        val time1 = currentTimeMillis()

        val root = Root(Hash(ByteArray(20)), dbUuid, time1, NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts() + pChen.toFacts() + mStonebreaker.toFacts() + eBrewer.toFacts()).toTypedArray()))
        val db = TestIndexer().index(root)
        assertArrayEquals(arrayOf(Gid(pChen.id!!)), db.query(attrIn(extId, 1, 3), attrIs(name, "Peter Chen")).map { it.gid }.toList().toTypedArray())
    }

    @JsName("Entity_with_multiple_values_of_list_attribute_should_be_returned_from_query_only_once")
    @Test
    fun `Entity with multiple values of list attribute should be returned from query only once`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                schemaMap.values.flatMap { it.toFacts() } + eCodd.toFacts()).toTypedArray()))
        val db = TestIndexer().index(root)
        assertArrayEquals(arrayOf(Gid(eCodd.id!!)), db.query(attrIn(nicks, "n", "u")).map { it.gid }.toList().toTypedArray())
    }

    @JsName("Indexer_can_index_multiple_transactions")
    @Test
    fun `Indexer can index multiple transactions`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val gids = Gid(eBrewer.id!!).nextGids()
        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((bootstrapSchema.values.flatMap { it.toFacts() } +
                testSchema.flatMap { testSchemaFactorizer.factor(it, bootstrapSchema::get, gids) } +
                extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.parentHash to root)
        val nodeResolver = mapNodeResolver(nodes)

        var db = TestIndexer(nodeResolver = nodeResolver).index(root)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData(pChen.toFacts().toList().toTypedArray()))
        nodes[n1.parentHash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData(mStonebreaker.toFacts().toList().toTypedArray()))
        nodes[n2.parentHash] = n2

        db = TestIndexer(baseDb = db, baseHash = root.parentHash, nodeResolver = nodeResolver).index(n2)
        assertNotNull(db.pull<Scientist>(eCodd.id!!))
        assertNotNull(db.pull<Scientist>(pChen.id!!))
        assertNotNull(db.pull<Scientist>(mStonebreaker.id!!))
    }

    @JsName("Indexer_can_index_updates")
    @Test
    fun `Indexer can index updates`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + eCodd.toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.parentHash to root)
        val nodeResolver = mapNodeResolver(nodes)
        var db = TestIndexer(nodeResolver = nodeResolver).index(root)

        val n1 = Leaf(Hash(byteArrayOf(1)), root, dbUuid, currentTimeMillis(), NodeData(pChen.toFacts().toList().toTypedArray()))
        nodes[n1.parentHash] = n1

        val n2 = Leaf(Hash(byteArrayOf(2)), n1, dbUuid, currentTimeMillis(), NodeData(pChen.copy( externalId = 5).toFacts().toList().toTypedArray()))
        nodes[n2.parentHash] = n2

        db = TestIndexer(baseDb = db, baseHash = root.parentHash, nodeResolver = nodeResolver).index(n2)
        assertNotNull(db.query(attrIs(extId, 5)))
    }

    @JsName("pull_with_fetch_eq_Eager_should_fetch_nullable_refs")
    @Test
    fun `pull with fetch = Eager should fetch nullable refs`() {
        val dbUuid = DbUuid(Iid(0, 1))

        val root = Root(Hash(ByteArray(20)), dbUuid, currentTimeMillis(), NodeData((extId.toFacts() + name.toFacts() + nicks.toFacts() + reviewer.toFacts() + country.toFacts() +
                Countries.name.toFacts() + Countries.population.toFacts() +
                eCodd.copy(reviewer = pChen).toFacts()).toTypedArray()))
        val nodes = hashMapOf<Hash, NodeVal<Hash>>(root.parentHash to root)
        val nodeResolver = mapNodeResolver(nodes)
        val db = TestIndexer(nodeResolver = nodeResolver).index(root)
        val pc = db.pull(Gid(eCodd.id!!), Scientist::class, Eager)!!
        assertNotNull(pc.reviewer)
    }

}