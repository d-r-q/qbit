package qbit

import qbit.model.*
import qbit.ns.Namespace
import qbit.storage.FileSystemStorage
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat

val cat = Namespace.of("q5", "category")
val trx = Namespace.of("q5", "transaction")
val trxSum = ScalarAttr(trx["sum"], DataType.QLong)
val trxDateTime = ScalarAttr(trx["dateTime"], DataType.QLong)
val trxCategory = RefAttr(trx["category"])
val trxComment = ScalarAttr(trx["comment"], DataType.QString)
val trxSource = ScalarAttr(trx["source"], DataType.QString)
val trxDevice = ScalarAttr(trx["device"], DataType.QString)
val catName = ScalarAttr(cat["name"], DataType.QString, true)

val categories = HashMap<String, IdentifiedEntity>()

const val datePattern = "dd.MM.yyyy"
const val timePattern = "HH:mm"

val dateTimeFormat = SimpleDateFormat("$datePattern $timePattern")

fun main(args: Array<String>) {
    var totalTime = 0L
    val iterations = 100
    for (i in 0..iterations) {
        println("Iteration $i/$iterations")
        val dbDir = File("/home/azhidkov/tmp/q5-db")
        if (dbDir.exists()) {
            println("Delete existing db")
            dbDir.deleteRecursively()
            dbDir.mkdir()
        }

        var start = System.currentTimeMillis()
        val conn = qbit(FileSystemStorage(dbDir.toPath()))
        var stop = System.currentTimeMillis()
        println("Open DB time: ${stop - start}")

        conn.persist(trxSum, trxDateTime, trxCategory, trxComment, trxSource, trxDevice, catName)
        start = System.currentTimeMillis()
        val dataFiles = File("/home/azhidkov/0my/Alive/qbit/q5").listFiles()
        dataFiles.forEach {
            it.forEachLine { line ->
                parse(conn, line) }
        }
        stop = System.currentTimeMillis()
        println("Persist time ${stop - start}")
        if (i > 2) {
            totalTime += (stop - start)
        }

        start = System.currentTimeMillis()
        val res = conn.db.query(attrIn(trxDateTime, dateTimeFormat.parse("01.06.2018 00:00").time, dateTimeFormat.parse("30.06.2018 23:59").time))
        stop = System.currentTimeMillis()
        println("Query time: ${stop - start}: ${res.size}")
    }
    println("Avg time: ${totalTime / (iterations - 2)}")
}

fun parse(conn: LocalConn, sourceLine: String) {
    val line = sourceLine
    val fieldsV1 = line
            .replace("\uFEFF", "") // Remove BOM
            .split("\",\"".toRegex())
            .map { it.trim('\"') }
    val fieldsV2 = line
            .replace("\uFEFF", "") // Remove BOM
            .split("\";\"".toRegex())
            .map { it.trim('\"') }
    val v1 = fieldsV1.size > fieldsV2.size
    val fields = if (v1) fieldsV1 else fieldsV2

    try {
        val date = if (fields.size > 0) fields[0] else return
        val time = if (fields.size > 1) fields[1] else return
        val dateTime = dateTimeFormat.parse("$date $time").time
        val sum = if (fields.size > 2) fields[2] else return
        val cat = if (fields.size > 3) fields[3] else return
        var catSe = categories[cat]
        if (catSe == null) {
            val stored = conn.persist(Entity(catName eq cat)).storedEntity()
            categories[cat] = stored
            catSe = stored
        }
        val comment = if (fields.size > 4) fields[4].replace("\"\"", "\"") else return
        val device = if (fields.size > 5) fields[5] else return
        val source = if (fields.size > 6) fields[6] else return
        val e = Entity(trxDateTime eq dateTime,
                trxSum eq (sum.replace(",", ".").toDouble() * 100).toLong(),
                trxCategory eq catSe,
                trxComment eq comment,
                trxDevice eq device,
                trxSource eq source)
        val (_, _) = conn.persist(e)
    } catch (e: ParseException) {
    }
}
