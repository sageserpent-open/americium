import cats.effect.IO
import cats.effect.unsafe.implicits.global
val aSimpleStream: fs2.Stream[IO, Int] =
  fs2.Stream.suspend(fs2.Stream.fromIterator[IO](Iterator.from(0), 10))
//fs2.Stream.iterate(0)(1 + _)

var dubiousMutableSignallingState: Boolean = false

def keepTrying(): fs2.Stream[IO, Int] = aSimpleStream.flatMap(stuff =>
  fs2.Stream
    .eval(IO { dubiousMutableSignallingState })
    .flatMap(restart =>
      if (restart)
        fs2.Stream
          .eval(IO {
            println("Restarting...")
            dubiousMutableSignallingState = false
          })
          .flatMap(_ => keepTrying())
      else fs2.Stream.emit[IO, Int](stuff)
    )
)

keepTrying()
  .take(50)
  .flatMap(stuff =>
    fs2.Stream.exec(IO {
      println(stuff)
      if (stuff % 7 == 6) { dubiousMutableSignallingState = true }
    })
  )
  .compile
  .drain
  .unsafeRunSync()
