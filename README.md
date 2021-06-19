# Undude

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
else? You have to undo every single step manually, like this:

```
// Declarations and blah blah

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
    try { fileSystem.delete(file) } catch (e: Throwable) { /* Take my life */ }
    return
}

```

And now you feel miserable because your code sucks.  
Here comes the Undude. With the Undude, you do this:

```
val u = Undude()
val response = u.execute( { anApi.doSomething() }, { r -> anApi.undoSomething(r.id) } )
val file = u.execute( { fileSystem.write(response.id) }, { f -> fileSystem.delete(f) } )
u.execute( { db.insert(response, file) }, {} )
```

The Undude takes care of undoing operation in reverse order as soon as one fails. This is 
prettier. And now you are happy again. And if you make your methods return an `Undoable<T>` object, 
with undo logic already enclosed in it, like this:

```
class Api {
    fun doSomething() = Undoable({ something() }, { r -> undoSomething(r.id) })
}
```

Then the code becomes even cleaner:

```
val u = Undude()
val response = u.execute( anApi.doSomething() )
val file = u.execute( fileSystem.write(response.id) )
u.execute( db.insert(response, file) )
```

This thing is bare bones, write me a line if you have a suggestion, I'll evolve it based on use
cases.


## Authors

Simone Paoletti