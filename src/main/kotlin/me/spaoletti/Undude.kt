package me.spaoletti

class Undude {

    private val undos = arrayListOf<Undo<*>>()

    private class Undo<T>(val ret: T, private val undo: (ret: T) -> Unit) {
        fun undo() = undo(ret)
    }

    fun <T> execute(action: () -> T, undo: (ret: T) -> Unit): T =
        try {
            val ret = action()
            undos.add(Undo(ret, undo))
            ret
        } catch (e: Throwable) {
            rollback()
            throw e
        }

    fun <T> execute(undoableAction: Undoable<T>): T =
        this.execute(undoableAction.action, undoableAction.undo)

    fun rollback() = undos.asReversed().forEach {
        try {
            it.undo()
        } catch (e: Throwable) {
            println("Something wrong happened while undoing(${it.ret}): ${e.message}")
        }
    }

}

class Undoable<T>(val action: () -> T, val undo: (ret: T) -> Unit)