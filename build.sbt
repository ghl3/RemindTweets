name := "remindtweets"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "3.0.3"

libraryDependencies += "postgresql" % "postgresql" % "8.4-702.jdbc4"

libraryDependencies += "com.typesafe.slick" %% "slick" % "2.0.0"

libraryDependencies += "com.typesafe.play" %% "play-slick" % "0.6.0.1"

libraryDependencies += "com.github.tminglei" % "slick-pg_2.10.1" % "0.2.2"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.5"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.5"

play.Project.playScalaSettings
