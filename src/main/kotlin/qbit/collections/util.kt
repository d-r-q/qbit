package qbit.collections


fun <E> List<E>.firstMatchIdx(c: (E) -> Int): Int {
    var idx = this.binarySearch { c(it) }
    while (idx > 0 && c(this[idx - 1]) == 0) {
        idx--
    }
    return idx
}

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
    assert(sorted(arr1, cmp))
    assert(sorted(arr2, cmp))
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
    assert(sorted(extended, cmp))
    return extended
}

fun <E : Any> replaceAll(arr: ArrayList<E>, el: List<E>, vararg indexes: Int): ArrayList<E> {
    assert(indexes.toSet().size == indexes.max()!! - indexes.min()!! + 1)
    val new = ArrayList(arr)
    indexes.sortedDescending().forEach { idx ->
        new.removeAt(idx)
    }
    new.addAll(indexes[0], el)
    new.trimToSize()
    return new
}

fun <E : Any> replace(arr: ArrayList<E>, els: ArrayList<E>, idx: Int): ArrayList<E> =
        replaceAll(arr, els, idx)

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

fun <E : Any> splitToTriple(arr: ArrayList<E>): Triple<ArrayList<E>, E, ArrayList<E>> {
    require(arr.size % 2 == 1)
    require(arr.size >= 3)

    val middle = arr.size / 2
    val left = ArrayList<E>(arr.subList(0, middle))
    val right = ArrayList<E>(arr.subList(middle + 1, arr.size))
    return Triple(left, arr[middle], right)
}

fun <E : Any> splitToPair(arr: ArrayList<E>): Pair<ArrayList<E>, ArrayList<E>> {
    require(arr.size >= 2)

    val middle = arr.size / 2
    val left = ArrayList<E>(arr.subList(0, middle))
    val right = ArrayList<E>(arr.subList(middle, arr.size))
    return Pair(left, right)
}

fun <E : Any> sorted(arr: ArrayList<E>, cmp: Comparator<E>): Boolean {
    for (i in 0 until arr.size - 1) {
        if (cmp.compare(arr[i], arr[i + 1]) > 0) {
            return false
        }
    }

    return true
}

