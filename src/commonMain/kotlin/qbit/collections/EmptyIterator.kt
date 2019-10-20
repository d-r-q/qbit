package qbit.collections

object EmptyIterator : Iterator<Nothing> {

    override fun hasNext(): Boolean {
        return false
    }

    override fun next(): Nothing {
        throw NoSuchElementException("There is no elements in empty iterator")
    }
}