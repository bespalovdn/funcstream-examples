package com.github.bespalovdn.funcstream.examples.stream

import com.github.bespalovdn.funcstream.FStream
import com.github.bespalovdn.funcstream.ext.TimeoutSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}
import scala.io.StdIn

class ConsoleStream extends FStream[String, String]
    with TimeoutSupport
{
    override def read(timeout: Duration): Future[String] = {
        val p = Promise[String]
        Future{
            val line = StdIn.readLine()
            p.success(line)
        }
        withTimeoutDo(timeout)(p.future)
    }

    override def write(elem: String): Future[Unit] = {
        val p = Promise[Unit]
        Future{
            println(elem)
            p.success(())
        }
        p.future
    }
}