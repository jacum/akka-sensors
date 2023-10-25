package akka.sensors

private[sensors] object MetricsTestUtils {
  val TestNameSpace: String        = "test_namespace"
  val TestSubSystem: String        = "test_subsystem"
  val builder: BasicMetricBuilders = BasicMetricBuilders.make(TestNameSpace, TestSubSystem)

  def asMetricName(in: String): String =
    s"${TestNameSpace}_${TestSubSystem}_$in"
}
