package me.spaoletti

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import kotlin.test.assertEquals

class UndudeTest {

    private class FakeException(message: String) : RuntimeException(message)

    private class FakeClient {
        fun insertSomething(id: Int): Int = id
        fun deleteSomething(id: Int) { println("$id is gone bye bye") }
        fun doSomethingWithNoReturn() {}
        fun undoSomethingWithNoArgs() {}
    }

    private fun buildFakeClientSpy(): FakeClient = spy(FakeClient())

    private fun suppress(doWrongStuff: () -> Unit) = try { doWrongStuff() } catch (andBeHappyWithIt: Throwable) {}

    @Test
    fun should_undo_previous_actions_if_the_new_one_throws() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute({ fakeClient.insertSomething(11) }, { id -> fakeClient.deleteSomething(id) })
        u.execute({ fakeClient.insertSomething(42) }, { id -> fakeClient.deleteSomething(id) })
        suppress { u.execute({ throw FakeException("crash") }, {}) }
        verify(fakeClient, times(1)).insertSomething(11)
        verify(fakeClient, times(1)).insertSomething(42)
        verify(fakeClient, times(1)).deleteSomething(11)
        verify(fakeClient, times(1)).deleteSomething(42)
    }

    @Test
    fun should_undo_previous_Undoable_types_if_the_new_one_throws() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute(Undoable({ fakeClient.insertSomething(11) }, { id -> fakeClient.deleteSomething(id) }))
        u.execute(Undoable({ fakeClient.insertSomething(42) }, { id -> fakeClient.deleteSomething(id) }))
        suppress { u.execute({ throw FakeException("boom") }, {}) }
        verify(fakeClient, times(1)).insertSomething(11)
        verify(fakeClient, times(1)).insertSomething(42)
        verify(fakeClient, times(1)).deleteSomething(11)
        verify(fakeClient, times(1)).deleteSomething(42)
    }

    @Test
    fun should_return_the_action_return_value() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        val ret = u.execute({ fakeClient.insertSomething(11) }, {})
        assertEquals(11, ret)
    }

    @Test
    fun should_be_happy_with_actions_not_returning_anything() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute({ fakeClient.doSomethingWithNoReturn() }, { fakeClient.undoSomethingWithNoArgs() })
        u.rollback()
        verify(fakeClient, times(1)).doSomethingWithNoReturn()
        verify(fakeClient, times(1)).undoSomethingWithNoArgs()
    }

    @Test
    fun should_not_undo_the_last_failed_action() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        suppress { u.execute({ throw FakeException("smash") }, { id: Int -> fakeClient.deleteSomething(id) }) }
        verify(fakeClient, times(0)).deleteSomething(any())
    }

    @Test(expected = FakeException::class)
    fun should_raise_the_exception_of_the_last_failed_action() {
        Undude().execute({ throw FakeException("crack") }, {})
    }

    @Test
    fun should_enclose_the_return_value_of_a_successful_action_in_its_undo_lambda() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute({ fakeClient.insertSomething(12) }, { id -> fakeClient.deleteSomething(id) })
        u.rollback()
        verify(fakeClient, times(1)).deleteSomething(12)
    }

    @Test
    fun should_rollback_every_action() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute({ fakeClient.insertSomething(12) }, { id -> fakeClient.deleteSomething(id) })
        u.execute({ fakeClient.insertSomething(96) }, { id -> fakeClient.deleteSomething(id) })
        u.execute({ fakeClient.insertSomething(44) }, { id -> fakeClient.deleteSomething(id) })
        u.rollback()
        verify(fakeClient, times(1)).deleteSomething(12)
        verify(fakeClient, times(1)).deleteSomething(96)
        verify(fakeClient, times(1)).deleteSomething(44)
    }

    @Test
    fun should_suppress_exceptions_during_rollback() {
        val fakeClient = buildFakeClientSpy()
        val u = Undude()
        u.execute({ fakeClient.insertSomething(12) }, { throw FakeException("clank") })
        u.execute({ fakeClient.insertSomething(96) }, { throw FakeException("bonk") })
        u.execute({ fakeClient.insertSomething(44) }, { throw FakeException("ouch") })
        u.rollback()
    }

}