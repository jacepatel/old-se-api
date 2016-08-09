name := "tructrac"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaJpa,
  cache,
  "com.newrelic.agent.java" % "newrelic-agent" % "3.15.0",
  "com.newrelic.agent.java" % "newrelic-api" % "3.15.0",
  "org.hibernate" % "hibernate-entitymanager" % "4.2.1.Final",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "mysql" % "mysql-connector-java" % "5.1.28",
  "com.braintreepayments.gateway" % "braintree-java" % "2.39.1",
  "org.codehaus.woodstox" % "woodstox-core-asl" % "4.1.4",
  "com.fasterxml" % "jackson-xml-databind" % "0.6.2",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.5.1"
)

play.Project.playJavaSettings
