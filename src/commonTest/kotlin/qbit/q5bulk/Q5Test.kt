package qbit.q5bulk

import qbit.*
import qbit.model.*
import qbit.ns.Namespace
import qbit.platform.*
import qbit.storage.FileSystemStorage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val cat = Namespace.of("q5", "category")
val trx = Namespace.of("q5", "transaction")
val trxSum = ScalarAttr(trx["sum"], QLong)
val trxDateTime = ScalarAttr(trx["dateTime"], QLong)
val trxCategory = RefAttr(trx["category"])
val trxComment = ScalarAttr(trx["comment"], QString)
val trxSource = ScalarAttr(trx["source"], QString)
val trxDevice = ScalarAttr(trx["device"], QString)
val catName = ScalarAttr(cat["name"], QString, true)

const val datePattern = "dd.MM.yyyy"
const val timePattern = "HH:mm"

val dateTimeFormat = SimpleDateFormat("$datePattern $timePattern")
val dateTimeFormatV1 = SimpleDateFormat("yy.MM.dd HH:mm")

class Q5Test {

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

        conn.persist(trxSum, trxDateTime, trxCategory, trxComment, trxSource, trxDevice, catName)
        val categories = HashMap<String, Entity<*>>()

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
                    val trxes = ArrayList<RoEntity<EID?>>()
                    file.forEachLine { line ->
                        val data = parse(line, categories)
                        data?.let { (trx, cat) ->
                            // actually it compiles
                            if (ZonedDateTimes.ofInstant(Instants.ofEpochMilli(trx[trxDateTime]), ZoneOffsets.ofHours(7)) in fileDate.rangeTo(nextDate)) {
                                trxes.add(trx)
                                lines++
                            }
                            device = trx[trxDevice]
                            assertNotNull(conn.db.query(attrIs(trxDateTime, trx[trxDateTime]), attrIs(trxDevice, trx[trxDevice])).firstOrNull())
                            assertNotNull(conn.db.query(attrIs(catName, cat[catName])).firstOrNull())
                        }
                    }
                    assertTrue(lines <= conn.db.query(attrIn(trxDateTime, fileDate.toInstant().toEpochMilli(), nextDate.toInstant().toEpochMilli()), attrIs(trxDevice, device)).count())
                }

    }

    private fun loadInSingleTrx(it: File, categories: HashMap<String, Entity<*>>, conn: LocalConn) {
        val trxes = ArrayList<RoEntity<EID?>>()
        it.forEachLine { line ->
            parse(line, categories)?.let { (trx, cat) ->
                val catName = trx[trxCategory][catName]
                if (catName !in categories) {
                    categories[catName] = cat
                }
                trxes.add(trx)
            }
        }
        trxes.addAll(categories.values)
        conn.persist(trxes).persistedEntities
                .filter { it.tryGet(catName) != null }
                .forEach {
                    categories[it[catName]] = it
                }
    }

    private fun loadInThreeTrxes(it: File, categories: HashMap<String, Entity<*>>, conn: LocalConn) {
        var trxes1 = ArrayList<Entity<EID?>>()
        var trxes2 = ArrayList<Entity<EID?>>()
        it.forEachLine { line ->
            parse(line, categories)?.let { (trx, cat) ->
                val catName = trx[trxCategory][catName]
                if (catName !in categories) {
                    categories[catName] = cat
                }
                if (trxes1.size <= trxes2.size) {
                    trxes1.add(trx)
                } else {
                    trxes2.add(trx)
                }
            }
        }
        val trx = conn.trx()
        val persistedCategories = trx.persist(categories.values).persistedEntities
        persistedCategories.forEach {
            categories[it[catName]] = it
        }
        categories.values.forEach {
            assertNotNull(trx.db.pull((it as AttachedEntity).eid))
        }
        trxes1 = trxes1.map {
            val catName = it[trxCategory][catName]
            it.with(trxCategory eq categories[catName]!!)
        }.toCollection(ArrayList())
        trxes2 = trxes2.map {
            val catName = it[trxCategory][catName]
            it.with(trxCategory eq categories[catName]!!)
        }.toCollection(ArrayList())
        trx.persist(trxes1)
        trx.persist(trxes2)
        trx.commit()
        categories.values.forEach {
            assertNotNull(conn.db.pull((it as AttachedEntity).eid))
        }
    }

    private fun loadInTrxPerLine(it: File, categories: HashMap<String, Entity<*>>, conn: LocalConn) {
        it.forEachLine { line ->
            parse(line, categories)?.let { (trx, cat) ->
                val catName = trx[trxCategory][catName]
                val (_, _, scat) = conn.persist(cat, trx)
                if (catName !in categories) {
                    categories[catName] = scat
                }
            }
        }
    }

    private fun parse(sourceLine: String, categories: Map<String, Entity<*>>): Pair<Entity<EID?>, Entity<EID?>>? {
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

        val date = if (fields.size > 0) fields[0] else return null
        val time = if (fields.size > 1) fields[1] else return null
        val dateTime =
                if (v1) dateTimeFormatV1.parse("$date $time").getTime()
                else dateTimeFormat.parse("$date $time").getTime()
        val sum = if (fields.size > 2) fields[2] else return null
        val cat = if (fields.size > 3) fields[3] else return null
        var catSe = categories[cat]
        if (catSe == null) {
            catSe = Entity(catName eq cat)
        }
        val comment = if (fields.size > 4) fields[4].replace("\"\"", "\"") else return null
        val device = if (fields.size > 5) fields[5] else return null
        val source = if (fields.size > 6) fields[6] else return null
        val e = Entity(trxDateTime eq dateTime,
                trxSum eq (sum.replace(",", ".").toDouble() * 100).toLong(),
                trxCategory eq catSe,
                trxComment eq comment,
                trxDevice eq device,
                trxSource eq source)
        return e to catSe
    }

}
