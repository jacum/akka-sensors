package akka.sensors.metered

import akka.actor.BootstrapSetup
import akka.actor.typed.{ActorSystem, DispatcherSelector, SpawnProtocol}
import akka.sensors.DispatcherMetrics
import akka.sensors.MetricsTestUtils.{asMetricName, builder}
import com.typesafe.config.ConfigFactory
import io.prometheus.client.CollectorRegistry
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._
import MeteredLogicSpec._
import akka.actor.setup.ActorSystemSetup

/**
 * This spec contains checks for metrics gathering implemented in .metered package.
 */
class MeteredLogicSpec extends AnyFreeSpec with Matchers {
  "Metered logic" - {
    "collects metrics for runnables" in {
      val cr      = new CollectorRegistry()
      val metrics = DispatcherMetrics.make(builder)
      metrics.allCollectors.foreach(cr.register)

      val withConfig  = BootstrapSetup(cfg)
      val withMetrics = MeteredDispatcherSetup(metrics)
      val setup       = ActorSystemSetup.create(withConfig, withMetrics)
      val actorSystem = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "test-system", setup)

      try {
        // here we get a metered dispatcher from a custom config
        // Avoid using it as the default dispatcher as it is going to be used by Akka itself.
        // In this case that usage will affect metrics we are testing
        val dispatcher = actorSystem.dispatchers.lookup(DispatcherSelector.fromConfig("our-test-dispatcher"))

        // check that samples in the metrics are not defined before running the test task
        val prevSamples = cr.metricFamilySamples().asIterator().asScala.toList.map(in => (in.name, in)).toMap
        prevSamples(asMetricName("queue_time_millis")).samples shouldBe empty
        prevSamples(asMetricName("run_time_millis")).samples shouldBe empty
        prevSamples(asMetricName("active_threads")).samples shouldBe empty
        prevSamples(asMetricName("thread_states")).samples shouldBe empty
        prevSamples(asMetricName("threads_total")).samples shouldBe empty
        prevSamples(asMetricName("executor_value")).samples shouldBe empty

        dispatcher.execute(() => Thread.sleep(3000))

        //Now we can check that these metrics contain some samples after 3 secs of execution
        val samples = cr.metricFamilySamples().asIterator().asScala.toList.map(in => (in.name, in)).toMap
        samples(asMetricName("queue_time_millis")).samples should not be empty
        samples(asMetricName("run_time_millis")).samples should not be empty
        samples(asMetricName("active_threads")).samples should not be empty
        samples(asMetricName("thread_states")).samples shouldBe empty
        samples(asMetricName("threads_total")).samples shouldBe empty
        samples(asMetricName("executor_value")).samples shouldBe empty
      } finally actorSystem.terminate()
    }
  }
}

object MeteredLogicSpec {
  private val cfgStr =
    """
      |our-test-dispatcher {
      |  type = "akka.sensors.metered.MeteredDispatcherConfigurator"
      |  instrumented-executor {
      |    delegate = "java.util.concurrent.ForkJoinPool"
      |    measure-runs = true
      |    watch-long-runs = false
      |  }
      |}
      |""".stripMargin

  private val cfg = ConfigFactory.parseString(cfgStr)
}
