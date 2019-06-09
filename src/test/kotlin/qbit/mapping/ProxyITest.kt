package qbit.mapping

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.*
import qbit.mapping.Trxes.primaryCategory
import qbit.mapping.Trxes.sums
import qbit.ns.ns
import qbit.schema.*
import qbit.storage.MemStorage


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

fun Category(name: String) = Category(Entity(Categories.name eq name))
class Category<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var name: String by AttrDelegate(Categories.name)

}

fun Trx(sums: List<Long>, primaryCategory: Category<*>, categories: List<Category<*>>) =
        Trx(Entity(Trxes.sums eq sums, Trxes.primaryCategory eq primaryCategory,
                Trxes.categories eq categories))

class Trx<E : EID?>(entity: Entity<E>) : TypedEntity<E>(entity) {

    var sums: List<Long> by ListAttrDelegate(Trxes.sums)

    val primaryCategory: Category<*> by RefAttrDelegate(Trxes.primaryCategory)

    fun primaryCategory(cat: Category<*>): Trx<E> {
        entity = entity.set(Trxes.primaryCategory, cat)
        return this
    }

    var categories: List<Category<*>> by RefListAttrDelegate(Trxes.categories)

}

class ProxyITest {

    @Test
    fun test() {
        val conn = qbit(MemStorage())
        conn.persist(Categories.name, sums, primaryCategory, Trxes.categories)
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
    }
}
