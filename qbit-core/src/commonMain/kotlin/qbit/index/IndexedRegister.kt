package qbit.index

import qbit.api.model.Hash

class IndexedRegister(
    val cells: List<Pair<Hash?, Any>>
) {
    fun indexValue(hash: Hash?, value: Any, causalHashes: List<Hash?>): IndexedRegister {
        val concurrentCells = cells.filter { !causalHashes.contains(it.first) }
        return IndexedRegister(concurrentCells + Pair(hash, value))
    }

    fun values() = cells.map { it.second }
}