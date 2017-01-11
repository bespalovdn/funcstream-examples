package com.github.bespalovdn.funcstream.examples.stream

import java.util.concurrent.{Executors, ThreadFactory}

import com.github.bespalovdn.funcstream.FStream
import com.github.bespalovdn.funcstream.ext.TimeoutSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.StdIn

class ConsoleStream extends FStream[String, String]
    with TimeoutSupport
{
    selfRef =>

    private val executionContext: ExecutionContext = {
        val factory  = new ThreadFactory () {
            override def newThread(r: Runnable): Thread = {
                val t = Executors.defaultThreadFactory().newThread(r)
                t.setDaemon(true)
                t
            }
        }
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1, factory))
    }

    override def read(timeout: Duration): Future[String] = {
        val p = Promise[String]
        Future{
            val line = selfRef.synchronized{ StdIn.readLine() }
            p.success(line)
        }(executionContext)
        withTimeoutDo(timeout)(p.future)
    }

    override def write(elem: String): Future[Unit] = {
        val p = Promise[Unit]
        Future{
            selfRef.synchronized{ println(elem) }
            p.success(())
        }(executionContext)
        p.future
    }
}
