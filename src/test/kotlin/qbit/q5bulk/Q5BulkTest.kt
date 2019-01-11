package qbit.q5bulk

import qbit.*
import qbit.ns.Namespace
import qbit.schema.RefAttr
import qbit.schema.ScalarAttr
import qbit.schema.eq
import qbit.storage.FileSystemStorage
import java.io.File
import java.text.SimpleDateFormat

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

fun main(args: Array<String>) {
    var totalTime = 0L
    val iterations = 100
    for (i in 0..iterations) {
        val dbDir = File("/home/azhidkov/tmp/q5-db")
        if (dbDir.exists()) {
            dbDir.deleteRecursively()
            dbDir.mkdir()
        }

        var start = System.currentTimeMillis()
        val conn = qbit(FileSystemStorage(dbDir.toPath()))
        var stop = System.currentTimeMillis()
        println("${stop - start}")

        conn.persist(trxSum, trxDateTime, trxCategory, trxComment, trxSource, trxDevice, catName)
        start = System.currentTimeMillis()
        val dataFiles = File("/home/azhidkov/0my/Alive/qbit/q5").listFiles()
        val categories = HashMap<String, Entity>()
        val trxes = ArrayList<Entity>()
        dataFiles.forEach {
            it.forEachLine { line ->
                parse(line, categories)?.let { (trx, cat) ->
                    val catName = trx[trxCategory]!![catName]!!
                    if (catName !in categories) {
                        categories[catName] = cat
                    }
                    trxes.add(trx)
                }
            }
        }
        stop = System.currentTimeMillis()
        println("parse time: ${stop - start}")

        start = System.currentTimeMillis()

        trxes.addAll(categories.values)
        conn.persist(trxes)
        stop = System.currentTimeMillis()
        println("persist time: ${stop - start}")
        if (i > 2) {
            totalTime += (stop - start)
        }

        start = System.currentTimeMillis()
        val res = conn.db.query(attrIn(trxDateTime, dateTimeFormat.parse("01.06.2018 00:00").time, dateTimeFormat.parse("30.06.2018 23:59").time))
        stop = System.currentTimeMillis()
        println("${stop - start}: ${res.size}")
    }
    println("Avg time: ${totalTime / (iterations - 2)}")
}

fun parse(sourceLine: String, categories: Map<String, Entity>): Pair<Entity, Entity>? {
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
    val dateTime = dateTimeFormat.parse("$date $time").time
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
