package qbit.collections

fun <E> arrayList(size: Int, init: (Int) -> E): ArrayList<E> {
    val res = ArrayList<E>(size)
    for (i in 1..size) {
        res.add(init(i))
    }
    return res
}

fun <E> List<E>.firstMatchIdx(c: (E) -> Int): Int {
    var idx = this.binarySearch { c(it) }
    while (idx > 0 && c(this[idx - 1]) == 0) {
        idx--
    }
    return idx
}

fun <E> List<E>.subList(from: Int): List<E> =
        this.subList(from, this.size)

// CoW ArrayList operations

fun <E : Any> set(arr: ArrayList<E>, el: E, idx: Int): ArrayList<E> {
    val new = ArrayList(arr)
    new[idx] = el
    return new
}

fun <E : Any> insert(arr: ArrayList<E>, el: E, idx: Int): ArrayList<E> =
        insert(arr, arrayListOf(el), idx)

fun <E : Any> insert(arr: ArrayList<E>, els: ArrayList<E>, idx: Int): ArrayList<E> {
    val new = ArrayList(arr)
    new.addAll(idx, els)
    new.trimToSize()
    return new
}

fun <E : Any> merge(arr: ArrayList<E>, el: E, cmp: Comparator<E>): ArrayList<E> =
        merge(arr, arrayListOf(el), cmp)

fun <E : Any> merge(arr1: ArrayList<E>, arr2: ArrayList<E>, cmp: Comparator<E>): ArrayList<E> {
    qbit.assert { sorted(arr1, cmp) }
    qbit.assert { sorted(arr2, cmp) }
    val extended = ArrayList<E>(arr1.size + arr2.size)
    var a1 = 0
    var a2 = 0
    for (i in 0 until arr1.size + arr2.size) {
        val e = when {
            a1 == arr1.size -> arr2[a2++]
            a2 == arr2.size -> arr1[a1++]
            cmp.compare(arr1[a1], arr2[a2]) <= 0 -> arr1[a1++]
            else -> arr2[a2++]
        }
        extended.add(e)
    }
    qbit.assert { sorted(extended, cmp) }
    return extended
}

fun <E : Any> replaceAll(arr: ArrayList<E>, el: List<E>, vararg indexes: Int): ArrayList<E> {
    qbit.assert { indexes.toSet().size == indexes.max()!! - indexes.min()!! + 1 }
    val new = ArrayList(arr)
    indexes.sortedDescending().forEach { idx ->
        new.removeAt(idx)
    }
    new.addAll(indexes[0], el)
    new.trimToSize()
    return new
}

fun <E : Any> replace(arr: ArrayList<E>, el: E, vararg indexes: Int): ArrayList<E> =
        replaceAll(arr, listOf(el), *indexes)

fun <E : Any> remove(arr: ArrayList<E>, vararg indexes: Int): ArrayList<E> {
    val new = ArrayList(arr)
    indexes.sortedDescending()
            .forEach { idx ->
                new.removeAt(idx)
            }
    return new
}

fun <E : Any> splitToPair(arr: ArrayList<E>): Pair<ArrayList<E>, ArrayList<E>> {
    require(arr.size >= 2)

    val middle = arr.size / 2
    val left = ArrayList<E>(arr.subList(0, middle))
    val right = ArrayList<E>(arr.subList(middle, arr.size))
    return Pair(left, right)
}

fun <E : Any> split(arr: ArrayList<E>, chunkSize: Int, minChunkSize: Int, maxChunkSize: Int): ArrayList<ArrayList<E>> {
    require(minChunkSize <= chunkSize && chunkSize <= maxChunkSize)

    val res = ArrayList(arr.asSequence()
            .chunked(chunkSize) { ArrayList(it) }
            .toList())
    while (res.last().size < minChunkSize) {
        val last = res.removeAt(res.size - 1)
        res.last().addAll(last)
    }
    if (res.last().size > maxChunkSize) {
        res.addAll(split(res.removeAt(res.size - 1), minChunkSize, minChunkSize, maxChunkSize))
    }
    return res
}

fun <E : Any> splitByArray(arr: Iterable<E>, splitter: List<E>, cmp: Comparator<E>): ArrayList<ArrayList<E>> {
    val res = arrayList(splitter.size + 1) { ArrayList<E>() }

    val iter = arr.iterator()
    var childIdx = 0
    while (iter.hasNext()) {
        val value = iter.next()
        while (childIdx < splitter.size && cmp.compare(value, splitter[childIdx]) >= 0) {
            childIdx++
        }
        res[childIdx].add(value)
    }

    return res
}

fun <E : Any> sorted(arr: ArrayList<E>, cmp: Comparator<E>): Boolean {
    for (i in 0 until arr.size - 1) {
        if (cmp.compare(arr[i], arr[i + 1]) > 0) {
            return false
        }
    }

    return true
}

