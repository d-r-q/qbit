package qbit.api

import kotlinx.serialization.Serializable


@Serializable
data class Category<P : Ref>(override val id: Eid, val name: String, val parent: P?) : Ref

@Serializable
data class Place(override val id: Eid, val name: String) : Ref
val noPlace: Eid? = null

@Serializable
data class Trx<C : Ref, P : Ref>(override val id: Eid, val sum: Long, val date: Long, val category: C, val place: P?) : Ref

@Serializable
data class CategoryTrxes<C : Ref>(val category: Category<C>, val trxes: List<Trx<C, Eid>>)

typealias EagerTrxAlias<C> = Trx<Category<C>, Place>

fun defineSchema() {
    schema {
        collection("Categories", Category.serializer(refSerializer)) {
            unique(Category<*>::parent, Category<*>::name, ident = true)
        }
        collection("Places", Place.serializer(), Place(tid(), "")) {
            unique(it::name)
        }
        collection("Trxes", Trx.serializer(refSerializer, refSerializer))

        view("CategoryTrxes", CategoryTrxes.serializer(refSerializer))

        migration("v2") {
            addAttr(Category<*>::parent)
        }
        migration("v2") {
            addAttrAlias(Category<*>::parent, "parent")
        }
    }
}

fun storeData() {
    val t3 = Trx(tid(), 0, 0, tid() as Ref, null as Eid?)
    val t4 = Trx(tid(), 0, 0, Category<Eid>(tid(), "", null), Place(tid(), ""))
    val t5 = Trx(tid(), 0, 0, Category<Category<Eid>>(tid(), "", null), noPlace)

    persist(t3, t4, Gid(0), t5.tombstone(), t3.copy(category = t3.category.tombstone()))
}

fun sqlQuery() {

}