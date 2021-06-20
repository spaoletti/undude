package me.spaoletti

import kotlinx.coroutines.runBlocking

class Undude {

    private val undos = arrayListOf<Undo<*>>()

    private class Undo<T>(val ret: T, private val undo: suspend (ret: T) -> Unit) {
        suspend fun undo() = undo(ret)
    }

    fun <T> execute(action: suspend () -> T, undo: suspend (ret: T) -> Unit): T = runBlocking {
        try {
            val ret = action()
            undos.add(Undo(ret, undo))
            ret
        } catch (e: Throwable) {
            rollback()
            throw e
        }
    }

    fun <T> execute(undoableAction: Undoable<T>): T =
        this.execute(undoableAction.action, undoableAction.undo)

    fun rollback() = runBlocking {
        undos.asReversed().forEach {
            try {
                it.undo()
            } catch (e: Throwable) {
                println("Something wrong happened while undoing(${it.ret}): ${e.message}")
            }
        }
    }

}

class Undoable<T>(val action: suspend () -> T, val undo: suspend (ret: T) -> Unit)