package qbit.q5bulk

import qbit.api.db.Conn
import qbit.factorization.attrName
import qbit.model.gid
import qbit.platform.*
import qbit.q5bulk.Trxes.dateTime
import qbit.qbit
import qbit.api.db.attrIn
import qbit.api.db.attrIs
import qbit.storage.FileSystemStorage
import qbit.schema.schema
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


const val datePattern = "dd.MM.yyyy"
const val timePattern = "HH:mm"

val dateTimeFormat = SimpleDateFormat("$datePattern $timePattern")
val dateTimeFormatV1 = SimpleDateFormat("yy.MM.dd HH:mm")

data class Category(val id: Long?, val name: String)

data class Trx(val id: Long?, val sum: Long, val dateTime: ZonedDateTime, val category: Category, val comment: String, val source: String, val device: String)

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

class Q5Test {

    @Ignore
    @Test
    fun test() {
        val dataDir = File("/home/azhidkov/0my/Alive/qbit/q5")
        if (!dataDir.exists()) {
            println("WARNING! Test is ignored since requirement is not satisfied")
            return
        }
        val dataFiles = dataDir.listFiles()
        val dbDir = File("/home/azhidkov/tmp/q5-db")
        if (dbDir.exists()) {
            dbDir.deleteRecursively()
            dbDir.mkdir()
        }

        val conn = qbit(FileSystemStorage(dbDir))

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
                    val fileDate = ZonedDateTimes.parse(file.getName().substringBefore("-") + "010000+0700", DateTimeFormatters.ofPattern("yyMMddHHmm[X]"))
                    val nextDate = fileDate.plusMonths(1)
                    val trxes = ArrayList<Trx>()
                    file.forEachLine { line ->
                        val data = parse(line, categories)
                        data?.let { trx ->
                            // actually it compiles
                            if (trx.dateTime in fileDate.rangeTo(nextDate)) {
                                trxes.add(trx)
                                lines++
                            }
                            device = trx.device
                            assertNotNull(conn.db().query(attrIs(dateTime, trx.dateTime), attrIs(Trxes.device, trx.device)).firstOrNull())
                            assertNotNull(conn.db().query(attrIs(Cats.name, trx.category.name)).firstOrNull())
                        }
                    }
                    val trx = conn.db().query(attrIn(dateTime, fileDate, nextDate), attrIs(Trxes.device, device)).count()
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
            assertNotNull(trx.db().pull(cat.gid!!))
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
            assertNotNull(conn.db().pull(it.gid!!))
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
                ZonedDateTimes.ofInstant(Instants.ofEpochMilli(dateTime), ZoneIds.of("Asia/Novosibirsk")),
                catSe,
                comment,
                source,
                device
        )
    }

}
