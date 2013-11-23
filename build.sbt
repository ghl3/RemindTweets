name := "remindtweets"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "3.0.3"

libraryDependencies +=   "postgresql" % "postgresql" % "8.4-702.jdbc4"

play.Project.playScalaSettings
