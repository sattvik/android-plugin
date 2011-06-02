import sbt._

class AndroidPlugin(info: ProjectInfo) extends PluginProject(info)
{
  override def managedStyle = ManagedStyle.Maven
  lazy val publishTo = Resolver.file("GitHub Pages", new java.io.File(System.getProperty("user.home"), "/devel/sattvik.github.com/maven"))

  val proguard = "net.sf.proguard" % "proguard" % "4.4"
  val ddmlib   = "com.google.android.tools" % "ddmlib" % "r10"
}
