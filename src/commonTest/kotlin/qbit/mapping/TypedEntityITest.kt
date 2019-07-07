package qbit.mapping


import qbit.*
import qbit.mapping.Trxes.primaryCategory
import qbit.mapping.Trxes.sums
import qbit.model.*
import qbit.ns.ns
import qbit.storage.MemStorage
import kotlin.test.Test
import kotlin.test.assertEquals


object Categories {
    val cat = ns("cat")
    val name = ScalarAttr(cat["name"], QString)
}

object Trxes {
    val trx = ns("trx")
    val sums = ListAttr(trx["sums"], QLong)
    val primaryCategory = RefAttr(trx["primaryCategory"])
    val categories = RefListAttr(trx["categories"])
}

object Nodes {
    val ns = ns("nodes")
    val next = RefAttr(ns["next"])
    val data = ScalarAttr(ns["data"], QString)
}

fun Category(name: String) = Entity(Categories.name eq name)
class Category<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Categories.name)

}

fun Trx(sums: List<Long>, primaryCategory: Category<*>, categories: List<Category<*>>) =
        Entity(Trxes.sums eq sums, Trxes.primaryCategory eq primaryCategory,
                Trxes.categories eq categories)

class Trx<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var sums: List<Long> by ListAttrDelegate(Trxes.sums)

    val primaryCategory: Category<*> by RefAttrDelegate(Trxes.primaryCategory)

    fun primaryCategory(cat: Category<*>): Trx<E> {
        entity = entity.with(Trxes.primaryCategory, cat)
        return this
    }

    var categories: List<Category<*>> by RefListAttrDelegate(Trxes.categories)

}

fun Node(data: String) = Node(Entity(Nodes.data eq data))
class Node<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var next: Node<*>? by RefAttrDelegate(Nodes.next)

    var data: String by AttrDelegate(Nodes.data)

}

class TypedEntityITest {

    @Test
    fun test() {
        val conn = qbit(MemStorage())
        conn.persist(Categories.name, sums, primaryCategory, Trxes.categories, Nodes.next, Nodes.data)
        conn.persist(Category("cat1"), Category("cat2"), Category("cat3"))
        val cat1 = conn.db.queryAs<Category<EID>>(attrIs(Categories.name, "cat1")).first()
        val cat2 = conn.db.queryAs<Category<EID>>(attrIs(Categories.name, "cat2")).first()
        val cat3 = conn.db.queryAs<Category<EID>>(attrIs(Categories.name, "cat3")).first()

        val t = Trx(listOf(10, 20), cat1, listOf(cat2))
        var trx = conn.persist(t).storedEntityAs<Trx<EID>>()
        assertEquals("cat1", trx.primaryCategory.name)
        assertArrayEquals(listOf(10L, 20L).toTypedArray(), trx.sums.toTypedArray())
        assertArrayEquals(listOf("cat2").toTypedArray(), trx.categories.map { it.name }.toTypedArray())

        trx.categories = listOf(cat2, cat1)
        assertArrayEquals(listOf(cat2, cat1).toTypedArray(), trx.categories.toTypedArray())
        trx = trx.primaryCategory(cat3)
        trx.sums = listOf(20, 30)
        trx = conn.persist(trx).storedEntityAs()

        assertEquals("cat3", trx.primaryCategory.name)
        assertArrayEquals(listOf(20L, 30L).toTypedArray(), trx.sums.toTypedArray())
        assertArrayEquals(listOf("cat2", "cat1").toTypedArray(), trx.categories.map { it.name }.toTypedArray())

        trx.sums = listOf(1)
        val origEid = trx.eid
        trx = conn.persist(trx).storedEntityAs()
        assertArrayEquals(arrayOf(1L), trx.sums.toTypedArray())
        assertEquals(origEid, trx.eid)

        trx.primaryCategory.name = "cat3.1"
        assertEquals("cat3.1", trx.primaryCategory.name)
        val pcEid = trx.primaryCategory.eid
        trx.categories[0].name = "cat2.1"
        assertEquals("cat2.1", trx.categories[0].name)
        val scEid = trx.categories[0].eid
        conn.persist(trx)
        assertEquals("cat3.1", conn.db.pullAs<Category<EID>>(pcEid!!)!!.name)
        assertEquals("cat2.1", conn.db.pullAs<Category<EID>>(scEid!!)!!.name)

        val n1 = Node("n1")
        val n2 = Node("n2")
        val n3 = Node("n3")
        n1.next = n2
        n2.next = n3
        n3.next = n1
        val n1Stored = conn.persist(n1, n2, n3).createdEntities[n1]!!.typed<EID, Node<EID>>()
        assertEquals("n1", n1Stored.data)
        assertEquals("n2", n1Stored.next?.data)
        assertEquals("n3", n1Stored.next?.next?.data)
        assertEquals("n1", n1Stored.next?.next?.next?.data)
        assertEquals(n1Stored.eid, n1Stored.next?.next?.next?.eid)
    }
}
