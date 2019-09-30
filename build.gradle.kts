plugins {
    // Apply the scala plugin to add support for Scala
    scala
    idea
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

val scalaVersion = "2.12.10"
val vertxVersion = "3.8.0"
val log4jVersion = "1.7.26"

dependencies {
    // Use Scala 2.12 in our library project
    implementation("org.scala-lang:scala-library:$scalaVersion")

    // VertX
    "io.vertx:vertx".let { v ->
        implementation("$v-lang-scala_2.12:$vertxVersion")
        implementation("$v-web-scala_2.12:$vertxVersion")
        implementation("$v-config-scala_2.12:$vertxVersion")
        implementation("$v-web-api-contract:$vertxVersion")
    }

    // Logging
    implementation("org.slf4j:slf4j-log4j12:$log4jVersion")
    implementation("com.nequissimus:zio-slf4j_2.12:0.3.0+3-3f772998")

    // Functional awesomeness
    implementation("org.typelevel:cats-core_2.12:2.0.0")
    implementation("dev.zio:zio_2.12:1.0.0-RC13")

    // Use Scalatest for testing our library
    testImplementation("junit:junit:4.12")
    testImplementation("org.scalatest:scalatest_2.12:3.0.8")

    // Need scala-xml at test runtime
    testRuntimeOnly("org.scala-lang.modules:scala-xml_2.12:1.2.0")
}
