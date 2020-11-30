package akka.sensors

import java.lang.management.{ManagementFactory, ThreadMXBean}
import java.util.concurrent.Executors

import akka.sensors.RunnableWatcher.stackTraceToString
import com.typesafe.scalalogging.LazyLogging

import scala.collection.concurrent.TrieMap
import scala.concurrent.BlockContext.withBlockContext
import scala.concurrent.duration._
import scala.concurrent.{BlockContext, CanAwait}
import scala.util.control.NonFatal

trait RunnableWatcher {
  def apply[T](f: => T): T
  def start(): () => Unit
}

object RunnableWatcher extends LazyLogging {

  type ThreadId = java.lang.Long

  type StartTime = java.lang.Long

  private lazy val threads = ManagementFactory.getThreadMXBean

  def apply(
    tooLongRunThreshold: Duration,
    checkInterval: Duration = 1.second,
    maxDepth: Int = 300,
    threads: ThreadMXBean = threads
  ): RunnableWatcher = {

    val cache = TrieMap.empty[ThreadId, StartTime]

    AkkaSensors.executor.scheduleWithFixedDelay(
      () =>
        try {
          val currentTime = System.nanoTime()
          for {
            (threadId, startTime) <- cache
            duration = (currentTime - startTime).nanos
            if duration >= tooLongRunThreshold
            _          <- cache.remove(threadId)
            threadInfo <- Option(threads.getThreadInfo(threadId, maxDepth))
          } {
            val threadName          = threadInfo.getThreadName
            val stackTrace          = threadInfo.getStackTrace
            val formattedStackTrace = stackTraceToString(stackTrace)
            logger.error(s"Detected a thread that is locked for ${duration.toMillis} ms: $threadName, current state:\t$formattedStackTrace")
          }
        } catch {
          case NonFatal(failure) => logger.error(s"failed to check hanging threads: $failure", failure)
        },
      checkInterval.length,
      checkInterval.length,
      checkInterval.unit
    )

    val startWatching = (threadId: ThreadId) => {
      val startTime = System.nanoTime()
      cache.put(threadId, startTime)
      ()
    }

    val stopWatching = (threadId: ThreadId) => {
      cache.remove(threadId)
      ()
    }

    apply(startWatching, stopWatching)
  }

  def apply(
    startWatching: RunnableWatcher.ThreadId => Unit,
    stopWatching: RunnableWatcher.ThreadId => Unit
  ): RunnableWatcher =
    new RunnableWatcher {

      def apply[T](f: => T): T = {
        val stop = start()
        try f
        finally stop()
      }

      def start(): () => Unit = {
        val threadId = Thread.currentThread().getId
        startWatching(threadId)
        () => stopWatching(threadId)
      }
    }

  def stackTraceToString(xs: Array[StackTraceElement]): String = xs.mkString("\tat ", "\n\tat ", "")

}
