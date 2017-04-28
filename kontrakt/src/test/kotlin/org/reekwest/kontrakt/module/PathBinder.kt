package org.reekwest.kontrakt.module

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.kontrakt.ContractBreach
import org.reekwest.kontrakt.Path
import org.reekwest.kontrakt.PathLens

abstract class PathBinder(val core: Core, vararg private val parts: PathLens<*>) {
    abstract infix operator fun <T> div(next: PathLens<T>): PathBinder

    open infix operator fun div(next: String): PathBinder = div(Path.fixed(next))

    internal fun from(path: BasePath): ExtractedParts? = try {
        if (path.toList().size == parts.size) {
            ExtractedParts(mapOf(*parts.mapIndexed { index, lens -> lens to path(index, lens) }.toTypedArray()))
        } else {
            null
        }
    } catch (e: ContractBreach) {
        null
    }

    companion object {
        data class Core(val route: Route, val method: Method, val pathFn: (BasePath) -> BasePath) {
            infix operator fun div(next: String) = copy(pathFn = { pathFn(it) / next })
            fun matches(actualMethod: Method, basePath: BasePath, actualPath: BasePath) = actualMethod == method && actualPath == pathFn(basePath)
        }
    }
}

class PathBinder0(core: Core) : PathBinder(core) {

    override infix operator fun div(next: String) = PathBinder0(core / next)

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder1(core, next)

    infix fun bind(fn: HttpHandler): ServerRoute = RouteBinder<() -> HttpHandler>(this, { fn, _ -> fn() }).bind({ fn })
}

class PathBinder1<out A>(core: Core,
                         private val psA: PathLens<A>) : PathBinder(core, psA) {

    override infix operator fun <T> div(next: PathLens<T>) = PathBinder2(core, psA, next)

    infix fun bind(fn: (A) -> HttpHandler): ServerRoute =
        RouteBinder<(A) -> HttpHandler>(this, { fn, parts -> fn(parts[psA]) }).bind(fn)
}

class PathBinder2<out A, out B>(core: Core,
                                private val psA: PathLens<A>,
                                private val psB: PathLens<B>) : PathBinder(core, psA, psB) {
    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")

    infix fun bind(fn: (A, B) -> HttpHandler): ServerRoute =
        RouteBinder<(A, B) -> HttpHandler>(this, { fn, parts -> fn(parts[psA], parts[psB]) }).bind(fn)
}
//
//class PathBinder3<out A, out B, out C>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
//                                       private val psA: PathLens<A>,
//                                       private val psB: PathLens<B>,
//                                       private val psC: PathLens<C>) : PathBinder {
//    override fun <T> div(next: PathLens<T>) = PathBinder4(route, pathFn, psA, psB, psC, next)
//
//    infix fun at(method: Method): RouteBinder<(A, B, C) -> HttpHandler> =
//        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC]) }, psA, psB, psC)
//}
//
//class PathBinder4<out A, out B, out C, out D>(override val route: Route, override val pathFn: (BasePath) -> BasePath,
//                                              private val psA: PathLens<A>,
//                                              private val psB: PathLens<B>,
//                                              private val psC: PathLens<C>,
//                                              private val psD: PathLens<D>) : PathBinder {
//    override fun <T> div(next: PathLens<T>) = throw UnsupportedOperationException("No support for longer paths!")
//
//    infix fun at(method: Method): RouteBinder<(A, B, C, D) -> HttpHandler> =
//        RouteBinder(this, method, { fn, parts -> fn(parts[psA], parts[psB], parts[psC], parts[psD]) }, psA, psB, psC, psD)
//}
