package qbit.api

import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1


@Serializable
data class Category<P : Ref>(override val id: Eid, val name: String, val parent: P?) : Entity

typealias EagerCategory = Category<Category<Ref>>

@Serializable
data class Place(override val id: Eid, val name: String) : Entity
val noPlace: Eid? = null

@Serializable
data class Trx<C : Ref, P : Ref>(override val id: Eid, val sum: Long, val date: Long, val category: C, val place: P?) : Entity

typealias SlimTrx = Trx<Eid, Eid>

typealias EagerTrx<C> = Trx<Category<C>, Place>

@Serializable
data class CategoryTrxes<C : Ref>(val category: Category<C>, val trxes: List<Trx<C, Eid>>)

@Serializable
data class CategoryTreeTrxes(val category: Category<Eid>, val children: List<CategoryTreeTrxes>, val trxes: List<EagerTrx<Eid>>)

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

        view("CategoryTreeTrxes", CategoryTreeTrxes.serializer())

        migration("v2") {
            addAttr(Category<*>::parent)
        }
        migration("v2") {
            addAttrAlias(Category<*>::parent, "parent")
        }
    }
}

suspend fun storeData() {
    val t3 = Trx(tid(), 0, 0, tid() as Ref, null as Eid?)
    val t4 = Trx(tid(), 0, 0, Category<Eid>(tid(), "", null), Place(tid(), ""))
    val t5 = Trx(tid(), 0, 0, Category<Category<Eid>>(tid(), "", null), noPlace)

    Qbit.persist(t3, t4, t5.tombstone(), t3.copy(category = Gid(0)), t3.copy(category = t3.category.tombstone()))
}

suspend fun categoryForPlace(place: String): Ref {
    // Нужен PreparedStatement
    val sql = """SELECT c.id
|                    FROM Trxes t 
|                      JOIN Categories c 
|                      JOIN Places p
|                GROUP BY c.cid
|                WHERE p.name = $place 
|                ORDER BY count(t.id) DESC LIMIT 1
    """.trimMargin()

    return Qbit.query(sql).first().attr("id")
}

fun trxesByDate(from: Long, to: Long): Flow<SlimTrx> {
    val ogSpec: GraphSpec<SlimTrx> = queryFor<SlimTrx>().where {
        SlimTrx::date between Pair(from, to)
    }
    return Qbit.query(ogSpec)
}

fun categoryTrxes2(cat: Ref): Flow<EagerTrx<Eid>> {
    val ogSpec = queryFor<Trx<Ref, Ref>>()
        .fetchPlace()
        .fetchCategory {
            fetchParentRec()
        }
    val root = queryFor<CategoryTreeTrxes>()
    return Qbit.query(root).transform { node -> node.children.rFlatMap { it.children to it.trxes } }
}

fun categoryTrxes(cat: Ref): Flow<EagerTrx<Eid>> {
    val ogSpec1 = queryFor<Trx<Ref, Ref>>()
        .fetchPlace()
        .fetchCategory {
            fetchParentRec()
        }
    val ogSpec: GraphSpec<CategoryTreeTrxes> = queryFor<CategoryTreeTrxes>().where {
        CategoryTreeTrxes::category eq cat
    }
    return Qbit.query(ogSpec).transform { node -> node.children.rFlatMap { it.children to it.trxes } }
}

private fun GraphSpec<Category<Ref>>.fetchParentRec(): GraphSpec<Category<Category<Ref>>> {
    TODO("Not yet implemented")
}

private fun <P : Ref, C : Ref> GraphSpec<Trx<Ref, P>>.fetchCategory(catSpec: GraphSpec<Category<Ref>>.() -> GraphSpec<C>): GraphSpec<Trx<C, P>> {
    TODO("Not yet implemented")
}

private fun <C : Ref> GraphSpec<Trx<C, Ref>>.fetchPlace(): GraphSpec<Trx<C, Place>> {
    return TODO("")
}

fun <T, R> List<T>.rFlatMap(op: (T) -> Pair<List<T>, List<R>>): List<R> {
    return this.flatMap { op(it).let { it.first.rFlatMap(op) + it.second} }
}

inline fun <reified T> queryFor(): GraphSpec<T> {
    TODO("Not yet implemented")
}

class GraphSpec<T> {

    fun where(criteria: GraphSpecBuilder<T>.() -> Unit): GraphSpec<T> {
        TODO("Not yet implemented")
    }

}

class GraphSpecBuilder<T> {

    infix fun <T> KProperty1<*, T>.between(limit: Pair<T, T>) {
        TODO("Not yet implemented")
    }

    infix fun <T> KProperty1<*, T>.eq(value: T) {
        TODO("Not yet implemented")
    }

}