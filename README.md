# The Undude

A variation of the command pattern.


## Description

So you are writing your microservice or whatever and your microservice or whatever, in order to 
complete, needs to call a RESTful endpoint, write a file based on the response of that endpoint, 
then write a row in a database with a reference to both the endpoint response and the file path.

```
val response = anApi.doSomething()
val file = fileSystem.write(response.id)
db.insert(response, file)
```

If one of these steps fails you have to undo everything, but how can you have a proper atomic 
transaction when neither the endpoint nor the filesystem nor the db give a s**t about you or anything
in your immediate proximity? You have to undo every single step manually.

```
try {
    response = anApi.doSomething()
} catch (e: EverythingWentWrongException) {
    return
}

try {
    file = fileSystem.write(response.id)
} catch (e: EverythingWentWrongAgainException) {
    try { anApi.undoSomething(response.id) } catch (e: Throwable) { /* What can I even do now? */ }
    return
}

try {
    db.insert(response, file)
} catch (e: OhMyGodWhyException) {
    try { anApi.undoSomething(response.id) } catch (e: Throwable) { /* I want to go home */ }
    try { fileSystem.delete(file) } catch (e: Throwable) { /* OMG take my life */ }
    return
}

```

And now you feel miserable because your code sucks.  
Here comes the Undude.

```
val u = Undude()
val response = u.execute( { anApi.doSomething() }, { r -> anApi.undoSomething(r.id) } )
val file = u.execute( { fileSystem.write(response.id) }, { f -> fileSystem.delete(f) } )
u.execute( { db.insert(response, file) }, {} )
```

Every Undude is a transaction. You give the Undude an action and a lambda that can undo that action.
The Undude takes care of undoing operations in reverse order as soon as one throws. And now you are happy again. 

You can also make your methods return `Undoable<T>` objects, with undo logic already enclosed in 
it.

```
class Api {
    fun doSomethingUndoable() = Undoable({ doSomething() }, { r -> undoSomething(r.id) })
}
```

Then the code becomes even cleaner:

```
val u = Undude()
val response = u.execute( anApi.doSomethingUndoable() )
val file = u.execute( fileSystem.undoableWrite(response.id) )
u.execute( db.undoableInsert(response, file) )
```

The Undude rollbacks automatically when there is an exception, but you can also rollback manually at any point.

```
val u = Undude()
val response = u.execute( anApi.doSomethingUndoable() )
val file = u.execute( fileSystem.undoableWrite(response.id) )
if (file.isNotPrettyEnough()) {
    u.rollback()
    return
}
val dbResponse = u.execute( db.undoableInsert(response, file) )
if (dbResponse.isNotWhatIExpected())
    u.rollback()
```

Coroutines are supported, so you can run suspending code inside your actions and undos. Keep in mind that every
`execute` and `rollback` blocks the thread until completed.

This thing is bare bones, write me a line if you have a suggestion, I'll evolve it based on use
cases.


## Authors

Simone Paoletti
