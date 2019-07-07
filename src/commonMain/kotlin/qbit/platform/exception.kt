package qbit.platform

expect open class IOException(message: String) : Exception

expect open class EOFException: IOException {
    constructor()
    constructor(message: String)
}