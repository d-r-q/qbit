package qbit.q5bulk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.serializersModuleOf
import qbit.api.db.*
import qbit.api.model.Attr
import qbit.api.system.Instance
import qbit.factorization.KSFactorization
import qbit.factorization.attrName
import qbit.api.model.impl.gid
import qbit.q5bulk.Trxes.dateTime
import qbit.qbit
import qbit.schema.schema
import qbit.storage.MemStorage
import qbit.test.model.FakeSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.contains
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.getValue
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toCollection
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val nskTz = ZoneId.of("Asia/Novosibirsk")
const val datePattern = "dd.MM.yyyy"
const val timePattern = "HH:mm"

val dateTimeFormat = SimpleDateFormat("$datePattern $timePattern")
val dateTimeFormatV1 = SimpleDateFormat("yy.MM.dd HH:mm")

@Serializable
data class Category(val id: Long?, val name: String)

@Serializable
data class Trx(val id: Long?, val sum: Long, val dateTime: Long, val category: Category, val comment: String, val source: String, val device: String)

val q5Schema = schema {
    entity(Category::class) {
        uniqueString(it::name)
    }
    entity(Trx::class)
}

val schemaMap = q5Schema.map { it.name to it}.toMap()

object Trxes {

    val dateTime = schemaMap.getValue(Trx::class.attrName(Trx::dateTime))
    val device = schemaMap.getValue(Trx::class.attrName(Trx::device))

}

object Cats {

    val name = schemaMap.getValue(Category::class.attrName(Category::name))

}

val types: Map<KClass<*>, KSerializer<*>> = mapOf(
    Attr::class to Attr.serializer(FakeSerializer<Any>()),
    Instance::class to Instance.serializer(),
    Trx::class to Trx.serializer(),
    Category::class to Category.serializer()
)

class Q5Test {

    @Test
    fun test() {
        val dataDir = File("/home/azhidkov/0my/Alive/qbit/q5")
        if (!dataDir.exists()) {
            println("WARNING! Test is ignored since requirement is not satisfied")
            return
        }
        val dataFiles = dataDir.listFiles()

        val conn = qbit(MemStorage(), KSFactorization(serializersModuleOf(types))::ksDestruct)

        q5Schema.forEach { conn.persist(it) }
        val categories = HashMap<String, Category>()

        dataFiles
                .filter { it.isFile() }
                .forEachIndexed { idx, file ->
                    when {
                        idx % 10 == 0 -> loadInTrxPerLine(file, categories, conn)
                        idx % 2 == 0 -> loadInSingleTrx(file, categories, conn)
                        else -> loadInThreeTrxes(file, categories, conn)
                    }
                    var lines = 0
                    var device = ""
                    val fileDate = ZonedDateTime.parse(file.getName().substringBefore("-") + "010000+0700", DateTimeFormatter.ofPattern("yyMMddHHmm[X]"))
                    val nextDate = fileDate.plusMonths(1)
                    val trxes = ArrayList<Trx>()
                    file.forEachLine { line ->
                        val data = parse(line, categories)
                        data?.let { trx ->
                            // actually it compiles
                            if (Instant.ofEpochMilli(trx.dateTime).atZone(nskTz) in fileDate.rangeTo(nextDate)) {
                                trxes.add(trx)
                                lines++
                            }
                            device = trx.device
                            assertNotNull(conn.db().query<Trx>(attrIs(dateTime, trx.dateTime), attrIs(Trxes.device, trx.device)).firstOrNull())
                            assertNotNull(conn.db().query<Category>(attrIs(Cats.name, trx.category.name)).firstOrNull())
                        }
                    }
                    val trx = conn.db().query<Trx>(attrIn(dateTime, fileDate.toInstant().toEpochMilli(), nextDate.toInstant().toEpochMilli()), attrIs(Trxes.device, device)).count()
                    assertTrue(lines <= trx, "$lines lines were imported, but $trx trxes found")
                }

    }

    private fun loadInSingleTrx(it: File, categories: HashMap<String, Category>, conn: Conn) {
        val trxes = ArrayList<Trx>()
        it.forEachLine { line ->
            parse(line, categories)?.let { trx ->
                val catName = trx.category.name
                if (catName !in categories) {
                    categories[catName] = conn.persist(trx.category).persisted!!
                }
                trxes.add(trx.copy(category = categories[catName]!!))
            }
        }
        categories.values.forEach { conn.persist(it)}
        val storedTrxes = trxes.map { conn.persist(it).persisted!! }.toList()
        storedTrxes.forEach {
            assertNotNull(it.category)
            categories[it.category.name] = it.category
        }
    }

    private fun loadInThreeTrxes(it: File, categories: HashMap<String, Category>, conn: Conn) {
        var trxes1 = ArrayList<Trx>()
        var trxes2 = ArrayList<Trx>()
        it.forEachLine { line ->
            parse(line, categories)?.let { trx ->
                val catName = trx.category.name
                if (catName !in categories) {
                    categories[catName] = trx.category
                }
                if (trxes1.size <= trxes2.size) {
                    trxes1.add(trx)
                } else {
                    trxes2.add(trx)
                }
            }
        }
        val trx = conn.trx()
        categories.values.forEach {
            val (cat) = trx.persist(it)
            categories[cat!!.name] = cat
            assertNotNull(trx.db().pull<Category>(cat.gid!!))
        }
        trxes1 = trxes1.map {
            it.copy(category = categories[it.category.name]!!)
        }.toCollection(ArrayList())
        trxes2 = trxes2.map {
            it.copy(category = categories[it.category.name]!!)
        }.toCollection(ArrayList())
        trxes1.forEach { trx.persist(it) }
        trxes2.forEach { trx.persist(it) }
        trx.commit()
        categories.values.forEach {
            assertNotNull(conn.db().pull<Category>(it.gid!!))
        }
    }

    private fun loadInTrxPerLine(it: File, categories: HashMap<String, Category>, conn: Conn) {
        it.forEachLine { line ->
            parse(line, categories)?.let { trx ->
                val catName = trx.category.name
                val (storedTrx) = conn.persist(trx)
                if (catName !in categories) {
                    categories[catName] = storedTrx!!.category
                }
            }
        }
    }

    private fun parse(sourceLine: String, categories: Map<String, Category>): Trx? {
        val fieldsV1 = sourceLine
                .replace("\uFEFF", "") // Remove BOM
                .split("\",\"".toRegex())
                .map { it.trim('\"') }
        val fieldsV2 = sourceLine
                .replace("\uFEFF", "") // Remove BOM
                .split("\";\"".toRegex())
                .map { it.trim('\"') }
        val v1 = fieldsV1.size > fieldsV2.size
        val fields = if (v1) fieldsV1 else fieldsV2

        val date = if (fields.isNotEmpty()) fields[0] else return null
        val time = if (fields.size > 1) fields[1] else return null
        val dateTime =
                if (v1) dateTimeFormatV1.parse("$date $time").getTime()
                else dateTimeFormat.parse("$date $time").getTime()
        val sum = if (fields.size > 2) fields[2] else return null
        val cat = if (fields.size > 3) fields[3] else return null
        var catSe = categories[cat]
        if (catSe == null) {
            catSe = Category(null, cat)
        }
        val comment = if (fields.size > 4) fields[4].replace("\"\"", "\"") else return null
        val device = if (fields.size > 5) fields[5] else return null
        val source = if (fields.size > 6) fields[6] else return null
        return Trx(null,
                (sum.replace(",", ".").toDouble() * 100).toLong(),
                dateTime,
                catSe,
                comment,
                source,
                device
        )
    }

}
