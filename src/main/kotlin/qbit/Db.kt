package qbit

import java.util.*

private typealias Entity = Map<String, Any>
private typealias Trx = Array<out Entity>
const val EID: String = "qbit/EID"

private const val DB_ID: String = "qbit/dbId"
private const val DBS: String = "qbit/dbs"

@Suppress("UNCHECKED_CAST")
class Db(private val dbUuid: String, private val dbId: Int) {

    private val seedVersion = VersionVector(0, listOf(0))

    private val log = TreeMap<VersionVector, Trx>()

    private lateinit var now: VersionVector

    init {
        store(seedVersion, arrayOf(mapOf(EID to DB_ID, "dbId" to 0)))
    }

    constructor() : this(UUID.randomUUID().toString(), 0) {
        store(seedVersion.next(1), arrayOf(mapOf(EID to DBS, dbUuid to dbId)))
        now = calcNow()
    }

    constructor(dbUuid: String, root: Db) : this(dbUuid, root.pull(DBS)!![dbUuid] as Int) {
        push(root, this, seedVersion)
    }

    fun create(e: Map<String, Any>): String {
        val eid = UUID.randomUUID().toString()
        val eided = e + (EID to eid)
        add(eided)
        return eid
    }

    fun add(e: Map<String, Any>) {
        now = now.next(countDbs())
        store(now, arrayOf(e))
    }

    fun add(eid: String, e: Map<String, Any>) {
        add(e + (EID to eid))
    }

    fun add(eid: String, vararg e: Pair<String, Any>) {
        add(mapOf(*e) + (EID to eid))
    }

    fun pull(eid: String): Map<String, Any>? {
        val res = HashMap<String, Any>()
        for (trx in log.descendingMap().entries) {
            // todo: транзакции надо так же пробегать в обратном порядке
            trx.value
                    .filter { it[EID] == eid }
                    .forEach {
                        for (e in it) {
                            res.putIfAbsent(e.key, e.value)
                        }
                    }
        }
        return if (res.containsKey(EID)) res else null
    }

    fun version() = now

    fun addDb(newDbUuid: String) {
        add(DBS, newDbUuid to countDbs())
    }

    fun sync(peer: Db) {
        val (src, dest) = if (dbId < peer.dbId) {
            Pair(this, peer)
        } else {
            Pair(peer, this)
        }
        push(src, dest, dest.now)
        push(dest, src, src.now)
    }

    private fun fetch(peer: Db) {
        push(peer, this, now)
    }

    private fun push(peer: Db) {
        push(this, peer, peer.now)
    }

    private fun push(from: Db, to: Db, dstVersion: VersionVector) {
        from.listAfter(dstVersion).forEach {
            to.store(it.first, it.second)
        }
        to.now = calcNow()
    }

    fun calcNow(): VersionVector {
        val newVersions = ArrayList<Long>()
        for (i in 1..countDbs()) newVersions.add(0)
        var newNow = VersionVector(dbId, newVersions)
        for (e in log.navigableKeySet().descendingSet()) {
            newNow = newNow.merge(e)
            if (!newNow.hasZero()) {
                break
            }
        }
        return newNow
    }

    private fun countDbs() = pull(DBS)!!.filterNot { it.key == EID }.size

    private fun listAfter(v: VersionVector): List<Pair<VersionVector, Trx>> {
        return log.filter { it.key.hasNovetyAfter(v) }
                .map { it.key to it.value }
    }

    private fun store(v: VersionVector, e: Trx) {
        log.put(v, e)
    }

}

class VersionVector(private val dbId: Int, private val versions: List<Long>) : Comparable<VersionVector> {

    internal fun next(dbsCount: Int): VersionVector {
        val newVersions = ArrayList<Long>()
        for (i in 0..dbsCount - 1) {
            when {
                i == dbId -> newVersions.add(versions[i] + 1)
                i < versions.size -> newVersions.add(versions[i])
                else -> newVersions.add(0)
            }
        }

        return VersionVector(dbId, newVersions)
    }

    internal fun merge(other: VersionVector): VersionVector {
        val newVersions = versions.zip(other.versions.extend(versions.size, 0)).map { Math.max(it.first, it.second) }
        return VersionVector(dbId, newVersions)
    }

    internal fun hasZero() = versions.any { it == 0L }

    fun hasNovetyAfter(other: VersionVector): Boolean {
        return when {
            versions.size > other.versions.size -> true
            else -> versions.zip(other.versions).any { it.first > it.second }
        }
    }

    override fun compareTo(other: VersionVector): Int {
        when {
            versions.size < other.versions.size -> return -1
            versions.size > other.versions.size -> return 1
        }

        val diff = versions.zip(other.versions)
                .dropWhile { it.first == it.second }
                .firstOrNull()
        return when {
            diff == null -> 0
            diff.first < diff.second -> -1
            diff.first > diff.second -> 1
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionVector

        if (dbId != other.dbId) return false
        if (versions != other.versions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dbId
        result = 31 * result + versions.hashCode()
        return result
    }

    override fun toString(): String {
        return "VersionVector(dbId=$dbId, versions=$versions)"
    }


}

private fun <T> List<T>.extend(size: Int, v: T): List<T> {
    val extended = ArrayList<T>(this)
    while (extended.size < size) {
        extended.add(v)
    }
    return extended
}
