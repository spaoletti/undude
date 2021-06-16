package me.spaoletti

class Undude {

    private val undos = arrayListOf<() -> Unit>()

    fun <T> execute(undoableAction:  Pair<() -> T, () -> Unit>): T =
        try {
            val ret = undoableAction.first()
            undos.add(undoableAction.second)
            ret
        } catch (e: Throwable) {
            undos.asReversed().forEach { it() }
            throw e
        }

    fun <T> execute(u: Undoable<T>): T = this.execute(u.dude to u.undude)

}

class Undoable<T>(val dude: () -> T, val undude: () -> Unit)