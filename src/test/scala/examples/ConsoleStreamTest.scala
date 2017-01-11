package examples

import com.github.bespalovdn.funcstream._
import com.github.bespalovdn.funcstream.examples.stream.ConsoleStream

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ConsoleStreamTest
{
    implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

    type Consumer[A] = FConsumer[String, String, String, String, A]

    def stream: FStream[String, String] = new ConsoleStream

    def test1(): Unit = {
        val printHello: Consumer[Unit] = FConsumer { implicit stream => for {
                _ <- stream.write("This is the test message. Press Enter to complete.")
                _ <- stream.read(timeout = 5.seconds)
            } yield consume()
        }
        val result = stream <=> printHello
        Await.ready(result, Duration.Inf)
    }
}

object ConsoleStreamTest extends App
{
    new ConsoleStreamTest().test1()

    println("DONE")
}