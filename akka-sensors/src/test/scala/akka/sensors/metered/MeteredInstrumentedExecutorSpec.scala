package akka.sensors.metered

import akka.ConfigurationException
import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.{ActorSystem, DispatcherSelector, SpawnProtocol}
import akka.sensors.DispatcherMetrics
import akka.sensors.metered.MeteredInstrumentedExecutorSpec._
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MeteredInstrumentedExecutorSpec extends AnyFreeSpec with Matchers {
  "MeteredInstrumentedExecutor" - {
    "is returned if configured(MeteredDispatcherSetup is defined)" in {
      val cr          = new PrometheusRegistry()
      val metrics     = DispatcherMetrics.makeAndRegister(cr)
      val withConfig  = BootstrapSetup(cfg)
      val withMetrics = MeteredDispatcherSetup(metrics)
      val setup       = ActorSystemSetup.create(withConfig, withMetrics)
      val actorSystem = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "test-system", setup)
      val dispatcher  = actorSystem.dispatchers.lookup(DispatcherSelector.defaultDispatcher())

      // do some execution to make sure that our executor is created
      dispatcher.execute(() => ())
    }

    "throws SetupNotFound if MeteredDispatcherSetup is not defined" in {
      val withConfig  = BootstrapSetup(cfg)
      val setup       = ActorSystemSetup.create(withConfig)
      def actorSystem = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "test-system", setup)

      val exception = the[IllegalArgumentException] thrownBy actorSystem
      exception.getCause shouldBe a[SetupNotFound]
    }
  }
}

object MeteredInstrumentedExecutorSpec {
  import com.typesafe.config.ConfigFactory
  private val cfgStr =
    """
      |akka.actor.default-dispatcher {
      |  executor = "akka.sensors.metered.MeteredInstrumentedExecutor"
      |  instrumented-executor {
      |    delegate = "fork-join-executor"
      |    measure-runs = false
      |    watch-long-runs = false
      |  }
      |}
      |""".stripMargin

  private val cfg = ConfigFactory.parseString(cfgStr)
}
