name := "remindtweets"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

play.Project.playScalaSettings



libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "3.0.3"