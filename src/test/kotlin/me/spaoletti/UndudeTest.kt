package me.spaoletti

import org.junit.Test

import org.junit.Assert.*

class UndudeTest {

    @Test
    fun test() {

        fun someStuff() =
            { println("do"); "" } to { println("undo") }

        fun someOtherStuff(result: String) =
            { if (result == "") println("do") else println("dohhh"); 51 } to
                    { println("undo") }

        val u = Undude()
        val result = u.execute(someStuff())
        val number = u.execute(someOtherStuff(result))
        u.execute({ println("do") } to { println("undo") })
        u.execute({ println("do") } to { println("undo") })

    }

}