package akka.sensors.metered

import akka.ConfigurationException
import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.{ActorSystem, DispatcherSelector, SpawnProtocol}
import akka.sensors.DispatcherMetrics
import akka.sensors.MetricsTestUtils._
import akka.sensors.metered.MeteredDispatcherConfiguratorSpec._
import com.typesafe.config.ConfigFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MeteredDispatcherConfiguratorSpec extends AnyFreeSpec with Matchers {
  "MeteredDispatcherConfigurator" - {
    "is returned if configured(MeteredDispatcherSetup is defined)" in {
      val metrics     = DispatcherMetrics.make(builder)
      val withConfig  = BootstrapSetup(cfg)
      val withMetrics = MeteredDispatcherSetup(metrics)
      val setup       = ActorSystemSetup.create(withConfig, withMetrics)
      val actorSystem = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "test-system", setup)
      val dispatcher  = actorSystem.dispatchers.lookup(DispatcherSelector.defaultDispatcher())

      try dispatcher shouldBe a[MeteredDispatcher]
      finally actorSystem.terminate()
    }

    "throws SetupNotFound if MeteredDispatcherSetup is not defined" in {
      val withConfig  = BootstrapSetup(cfg)
      val setup       = ActorSystemSetup.create(withConfig)
      def actorSystem = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "test-system", setup)

      val exception = the[ConfigurationException] thrownBy actorSystem
      exception.getCause shouldBe a[SetupNotFound]
    }
  }
}

object MeteredDispatcherConfiguratorSpec {
  private val cfgStr =
    """
      |akka.actor.default-dispatcher {
      |  type = "akka.sensors.metered.MeteredDispatcherConfigurator"
      |  instrumented-executor {
      |    delegate = "java.util.concurrent.ForkJoinPool"
      |    measure-runs = false
      |    watch-long-runs = false
      |  }
      |}
      |""".stripMargin

  private val cfg = ConfigFactory.parseString(cfgStr)
}
