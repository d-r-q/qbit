package qbit.collections


class QIterator<E>(private val iter: Iterator<E>) {

    var peek: E? = if (iter.hasNext()) iter.next() else null

    fun next(): E? {
        val next = peek
        peek = if (iter.hasNext()) iter.next() else null
        return next
    }

    fun takeWhile(pred: (E) -> Boolean): Iterator<E> {
        return object : Iterator<E> {
            override fun hasNext(): Boolean {
                return peek != null && pred(peek!!)
            }

            override fun next(): E {
                return this@QIterator.next() ?: throw NoSuchElementException()
            }
        }
    }

}