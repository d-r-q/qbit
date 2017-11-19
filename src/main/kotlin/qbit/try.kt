package qbit

sealed class Try<out R> {
    abstract val isOk: Boolean
    abstract val isErr: Boolean
    abstract val res: R

    inline fun <R2> mapOk(f: (R) -> R2): Try<R2> = when (this) {
        is Ok -> ok(f(this.res))
        is Err<*> -> this
    }

    inline fun <R2> ifOkTry(f: (R) -> Try<R2>): Try<R2> = when (this) {
        is Ok -> f(res)
        is Err<*> -> this
    }

    inline fun <reified T> its(): Boolean = when (this) {
        is Ok -> res is T
        is Err<*> -> reason is T
    }

    inline fun <E2 : Throwable> mapErr(f: (Throwable) -> E2): Try<R> = when (this) {
        is Ok -> this
        is Err<*> -> err(f(this.reason))
    }

}

class Ok<out R>(override val res: R) : Try<R>() {
    override val isOk: Boolean = true
    override val isErr: Boolean = false
    override fun toString(): String = "ok($res)"
}

class Err<out E : Throwable>(val reason: E) : Try<Nothing>() {
    override val isOk: Boolean = false
    override val isErr: Boolean = true
    override val res get() = throw reason
    override fun toString() = reason.toString()
}

fun <R> ok(res: R) = Ok(res)
fun <E : Throwable> err(reason: E) = Err(reason)

inline fun <T> Try(body: () -> T): Try<T> =
        TTry<T> {
            ok(body())
        }

inline fun <T> TTry(body: () -> Try<T>): Try<T> =
        try {
            body()
        } catch (e: Throwable) {
            err(e)
        }


fun <A1, R> ifOk(p: Try<A1>, body: (A1) -> Try<R>) = when (p) {
    is Ok -> body(p.res)
    is Err<*> -> p
}

fun <A1, A2, R> ifOk(a1: Try<A1>, a2: Try<A2>, body: (A1, A2) -> R): Try<R> =
        if (a1.isOk && a2.isOk) ok(body(a1.res, a2.res))
        else listOf(a1, a2).first { it.isErr } as Try<R>

fun <A1, A2, A3, R> ifOk(a1: Try<A1>, a2: Try<A2>, a3: Try<A3>, body: (A1, A2, A3) -> R): Try<R> =
        if (a1.isOk && a2.isOk && a3.isOk) ok(body(a1.res, a2.res, a3.res))
        else listOf(a1, a2, a3).first { it.isErr } as Try<R>

fun <A1, A2, A3, A4, A5, R> ifOk(a1: Try<A1>, a2: Try<A2>, a3: Try<A3>, a4: Try<A4>, a5: Try<A5>, body: (A1, A2, A3, A4, A5) -> R): Try<R> =
        if (a1.isOk && a2.isOk && a3.isOk && a4.isOk && a5.isOk) ok(body(a1.res, a2.res, a3.res, a4.res, a5.res))
        else listOf(a1, a2, a3, a4, a5).first { it.isErr } as Try<R>

fun <A1, A2, A3, A4, A5, A6, R> ifOk(a1: Try<A1>, a2: Try<A2>, a3: Try<A3>, a4: Try<A4>, a5: Try<A5>, a6: Try<A6>, body: (A1, A2, A3, A4, A5, A6) -> R): Try<R> =
        if (a1.isOk && a2.isOk && a3.isOk && a4.isOk && a5.isOk && a6.isOk) ok(body(a1.res, a2.res, a3.res, a4.res, a5.res, a6.res))
        else listOf(a1, a2, a3, a4, a5, a6).first { it.isErr } as Try<R>

fun <R> Sequence<Try<R>>.flatten(): Try<List<R>> {
    val res = ArrayList<R>()
    for (r in this) {
        when (r) {
            is Ok -> res.add(r.res)
            is Err<*> -> return r
        }
    }
    return ok(res)
}
