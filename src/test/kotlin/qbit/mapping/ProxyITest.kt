package qbit.mapping

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import qbit.*
import qbit.ns.ns
import qbit.schema.*
import qbit.storage.MemStorage


val cat = ns("cat")
val catName = ScalarAttr(cat["name"], QString)

val trx = ns("trx")
val trxSums = ListAttr(trx["sums"], QLong)
val primaryCategory = RefAttr(trx["primaryCategory"])
val categories = RefListAttr(trx["categories"])

fun Category(cname: String) = proxy<Category>(Entity(catName eq cname))

interface Category : EntityHolder {

    var name: String

}

fun Trx(tsums: List<Long>, primCat: Category, cats: List<Category>) = proxy<Trx>(Entity(
        trxSums eq tsums,
        primaryCategory eq primCat.entity(),
        categories eq cats.map { it.entity() }))

interface Trx : EntityHolder {

    var sums: List<Long>

    val primaryCategory: Category
    fun primaryCategory(cat: Category): Trx

    var categories: List<Category>

}

class ProxyITest {

    @Test
    fun test() {
        val conn = qbit(MemStorage())
        conn.persist(catName, trxSums, primaryCategory, categories)
        conn.persist(Category("cat1").entity(), Category("cat2").entity(), Category("cat3").entity())
        val cat1 = conn.db.queryAs<Category>(attrIs(catName, "cat1")).first()
        val cat2 = conn.db.queryAs<Category>(attrIs(catName, "cat2")).first()
        val cat3 = conn.db.queryAs<Category>(attrIs(catName, "cat3")).first()

        var trx = proxy<Trx>(conn.persist(Trx(listOf(10, 20), cat1, listOf(cat2)).entity()).storedEntity())
        assertEquals("cat1", trx.primaryCategory.name)
        assertArrayEquals(listOf(10L, 20L).toTypedArray(), trx.sums.toTypedArray())
        assertArrayEquals(listOf("cat2").toTypedArray(), trx.categories.map { it.name }.toTypedArray())

        trx.categories = listOf(cat2, cat1)
        assertArrayEquals(listOf(cat2.entity(), cat1.entity()).toTypedArray(), trx.categories.map { it.entity() }.toTypedArray())
        trx = trx.primaryCategory(cat3)
        trx.sums = listOf(20, 30)
        trx = proxy(conn.persist(trx.entity()).storedEntity())

        assertEquals("cat3", trx.primaryCategory.name)
        assertArrayEquals(listOf(20L, 30L).toTypedArray(), trx.sums.toTypedArray())
        assertArrayEquals(listOf("cat2", "cat1").toTypedArray(), trx.categories.map { it.name }.toTypedArray())
    }
}