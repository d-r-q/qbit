package qbit

import qbit.ns.Namespace
import qbit.schema.Attr
import qbit.storage.FileSystemStorage
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat

val cat = Namespace.of("q5", "category")
val trx = Namespace.of("q5", "transaction")
val trxSum = Attr(trx["sum"], QLong)
val trxDateTime = Attr(trx["dateTime"], QLong)
val trxCategory = Attr(trx["category"], QEID)
val trxComment = Attr(trx["comment"], QString)
val trxSource = Attr(trx["source"], QString)
val trxDevice = Attr(trx["device"], QString)
val catName = Attr(cat["name"], QString, true)

val categories = HashMap<String, StoredEntity>()

val datePattern = "dd.MM.yyyy"
val timePattern = "HH:mm"

val dateTimeFormat = SimpleDateFormat("$datePattern $timePattern")

fun main(args: Array<String>) {
    val dbDir = File("/tmp/q5-db")
    if (dbDir.exists()) {
        dbDir.deleteRecursively()
        dbDir.mkdir()
    }

    val conn = qbit(FileSystemStorage(dbDir.toPath()))
    conn.persist(trxSum, trxDateTime, trxCategory, trxComment, trxSource, trxDevice, catName)

    val dataFiles = File("/home/azhidkov/0my/Alive/qbit/q5").listFiles()
    dataFiles.forEach {
        it.forEachLine { line ->
            parse(conn, line)
        }
    }

    val start = System.currentTimeMillis()
    val res = conn.db.query(attrIn(trxDateTime, dateTimeFormat.parse("01.06.2018 00:00").time, dateTimeFormat.parse("30.06.2018 23:59").time))
    val stop = System.currentTimeMillis()
    println("${stop - start}")
}

fun parse(conn: LocalConn, sourceLine: String) {
    val line = sourceLine
    val fieldsV1 = line
            .replace("\uFEFF", "") // удаление BOM-ов
            .split("\",\"".toRegex())
            .map { it.trim('\"') }
    val fieldsV2 = line
            .replace("\uFEFF", "") // удаление BOM-ов
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
            conn.db.query(attrIs(catName, cat))
            val (_, stored) = conn.persist(Entity(catName to cat))
            categories[cat] = stored
            catSe = stored
        }
        val comment = if (fields.size > 4) fields[4].replace("\"\"", "\"") else return
        val device = if (fields.size > 5) fields[5] else return
        val source = if (fields.size > 6) fields[6] else return
        val e = Entity(trxDateTime to dateTime,
                trxSum to (sum.replace(",", ".").toDouble() * 100).toLong(),
                trxCategory to catSe.eid,
                trxComment to comment,
                trxDevice to device,
                trxSource to source)
        conn.persist(e)
    } catch (e: ParseException) {
    }
}
