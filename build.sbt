ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.remisiki"
ThisBuild / organizationName := "remisiki"
ThisBuild / scalacOptions    ++= Seq(
	"-unchecked",
	"-deprecation",
	"-feature",
	// "-Werror",
	// "-Xlint",
)

lazy val root = (project in file("."))
	.settings(
		name := "illust-tools",
		Compile / scalaSource := baseDirectory.value / "src"
	)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

libraryDependencies ++= Seq(
	"com.remisiki" % "browser-cookies" % "0.1.1",
	"org.json" % "json" % "20220924",
	"org.jsoup" % "jsoup" % "1.15.1",
	"org.slf4j" % "slf4j-simple" % "2.0.6",
	"org.xerial" % "sqlite-jdbc" % "3.36.0.3",
)

assembly / assemblyMergeStrategy := {
	case r if r.startsWith("reference.conf")          => MergeStrategy.concat
	case manifest if manifest.contains("MANIFEST.MF") =>
		// We don't need manifest files since sbt-assembly will create
		// one with the given settings
		MergeStrategy.discard
	case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
		// Keep the content for all reference-overrides.conf files
		MergeStrategy.concat
	case PathList("META-INF", xs @ _*) => MergeStrategy.discard
	case x => MergeStrategy.first
	// case x =>
	//  // For all the other files, use the default sbt-assembly merge strategy
	//  val oldStrategy = (assemblyMergeStrategy in assembly).value
	//  oldStrategy(x)
}