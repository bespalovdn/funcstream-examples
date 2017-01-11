package examples

import com.github.bespalovdn.funcstream._
import com.github.bespalovdn.funcstream.examples.stream.ConsoleStream

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConsoleStreamTest
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
        waitResult(result)
    }

    def test2(): Unit = {
        // string => int transformer:
        val readInt: FPipe[String, String, Int, String] = FPipe{ upStream =>
            new FStream[Int, String] {
                override def read(timeout: Duration): Future[Int] = upStream.read(timeout).map(_.toInt)
                override def write(elem: String): Future[Unit] = upStream.write(elem)
            }
        }
        val adder: FPlainConsumer[Int, String, Unit] = FConsumer{ implicit stream => for{
                _ <- stream.write("Input some number to summarize: ")
                n <- stream.read()
                _ <- stream.write(s"$n + $n = " + (n + n))
            } yield consume()
        }
        val multiplier: FPlainConsumer[Int, String, Unit] = FConsumer{ implicit stream => for{
                _ <- stream.write("Input some number to multiply: ")
                n <- stream.read()
                _ <- stream.write(s"$n * $n = " + (n * n))
            } yield consume()
        }
        val result = stream <=> readInt <=> (adder >> multiplier)
        waitResult(result)
    }

    def waitResult[A](f: Future[A]) = {
        Await.ready(f, Duration.Inf)
        f.value match {
            case Some(Success(_)) => println("Success")
            case Some(Failure(t)) => println("Failed with: " + t.getMessage)
            case _ => throw new IllegalStateException()
        }
    }
}

object ConsoleStreamTest extends App
{
    new ConsoleStreamTest().test2()
}