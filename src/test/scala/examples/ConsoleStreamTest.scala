package examples

import com.github.bespalovdn.funcstream._
import com.github.bespalovdn.funcstream.examples.stream.ConsoleStream

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConsoleStreamTest extends FutureExtensions
{
    implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

    def stream: FStream[String, String] = new ConsoleStream

    def test1(): Unit = {
        type Consumer[A] = FConsumer[String, String, String, String, A]
        val greeting: Consumer[Unit] = FConsumer { implicit stream => for {
                _ <- stream.write("This is the test message. Press Enter to complete.")
                _ <- stream.read(timeout = 5.seconds)
            } yield consume()
        }
        val result = stream <=> greeting
        waitFor(result)
    }

    def test2(): Unit = {
        type Consumer[A] = FPlainConsumer[Int, String, A]
        // string => int transformer:
        val readInt: FPipe[String, String, Int, String] = FPipe{ upStream =>
            new FStream[Int, String] {
                override def read(timeout: Duration): Future[Int] = upStream.read(timeout).map(_.toInt)
                override def write(elem: String): Future[Unit] = upStream.write(elem)
            }
        }
        val adder: Consumer[Unit] = FConsumer{ implicit stream => for{
                _ <- stream.write("Input some number to summarize: ")
                n <- stream.read()
                _ <- stream.write(s"$n + $n = " + (n + n))
            } yield consume()
        }
        val multiplier: Consumer[Unit] = FConsumer{ implicit stream => for{
                _ <- stream.write("Input some number to multiply: ")
                n <- stream.read()
                _ <- stream.write(s"$n * $n = " + (n * n))
            } yield consume()
        }
        def prompt(msg: String): Consumer[Unit] = FConsumer { implicit stream =>
            stream.write(msg) >> success(consume())
        }
        /*def repeat(c: Consumer[Unit], nTimes: Int): Consumer[Unit] = FConsumer { implicit stream =>
            if(nTimes > 0) c.apply(stream) >> repeat(c, nTimes - 1).apply(stream)
            else success(consume())
        }*/
        // build the pipeline and run:
        val result = stream <=> readInt <=> {
            prompt("This is some kind of calculator. Please follow the instructions:") >>
            adder >>
            multiplier >>
            prompt("That's it!")
        }
        waitFor(result)
    }

    def waitFor[A](f: Future[A]) = {
        Await.ready(f, Duration.Inf)
        f.value match {
            case Some(Success(_)) => println("Success")
            case Some(Failure(t)) => println("Failed with %s: %s" format(t.getClass.getSimpleName, t.getMessage))
            case _ => throw new IllegalStateException()
        }
    }
}

object ConsoleStreamTest extends App
{
    new ConsoleStreamTest().test2()
}