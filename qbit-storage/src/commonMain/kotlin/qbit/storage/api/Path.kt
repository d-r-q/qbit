package qbit.storage.api


data class Path(val els: List<String>) {

    fun resolve(other: Path) =
            Path(els + other.els)

}

