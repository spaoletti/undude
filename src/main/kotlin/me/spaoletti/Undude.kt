package me.spaoletti

import kotlinx.coroutines.runBlocking

class Undude {

    class UndudeException(val original: Exception, val undoException: Exception): Exception() {
    }
    
    private class Undo<T>(val ret: T, private val undo: suspend (ret: T) -> Unit) {
        suspend fun undo() = undo(ret)
    }

    fun <T> execute(action: suspend () -> T, undo: suspend (ret: T) -> Unit): T = runBlocking {
        try {
            return action()
        } catch (e: Throwable) {
            try {
                undo()
            } catch (undoException: Throwable) {
                throw UndudeException(e, undoException)
            }
            throw e
        }
    }
}
