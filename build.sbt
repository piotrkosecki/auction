name := "auctions"

version := "1.0"

lazy val `auctions` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

val akkaVersion = "2.4.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-persistence-query-experimental"  % akkaVersion,
  "com.typesafe.akka"         %% "akka-persistence"                     % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor"                           % akkaVersion,
  "com.typesafe.akka"         %% "akka-stream"                          % akkaVersion,
  "com.github.dnvriend"       %% "akka-persistence-inmemory"            % "2.4.18.1",
  "com.typesafe.slick"        %% "slick"                                % "3.2.0",
  "com.typesafe.slick"        %% "slick-hikaricp"                       % "3.2.0",
  "com.h2database"            %  "h2"                                   % "1.4.191",
  jdbc,
  cache,
  ws,
  specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

