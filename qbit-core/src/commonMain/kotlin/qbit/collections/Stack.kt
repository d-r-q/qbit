package qbit.collections

inline class Stack<T : Any>(private val arrayList: ArrayList<T> = ArrayList()) : Collection<T> {

    fun push(e: T) {
        arrayList.add(e)
    }

    fun peek(): T =
        arrayList.takeIf { it.isNotEmpty() }?.last() ?: throw NoSuchElementException("The stack is empty")

    fun pop(): T =
        arrayList.takeIf { it.isNotEmpty() }?.removeAt(arrayList.size - 1)
            ?: throw NoSuchElementException("The stack is empty")

    override val size: Int
        get() = arrayList.size

    override fun contains(element: T): Boolean =
        arrayList.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean =
        arrayList.containsAll(elements)

    override fun isEmpty(): Boolean =
        arrayList.isEmpty()

    override fun iterator(): Iterator<T> =
        arrayList.iterator()

}