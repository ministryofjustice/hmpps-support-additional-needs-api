import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask


plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.2"
  kotlin("plugin.spring") version "2.2.20"
  kotlin("plugin.jpa") version "2.2.20"
  id("org.openapi.generator") version "7.16.0"

  `java-test-fixtures`
}

apply(plugin = "org.openapi.generator")

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val postgresqlVersion = "42.7.8"
val kotlinLoggingVersion = "3.0.5"
val testContainersVersion = "1.21.3"
val buildDirectory: Directory = layout.buildDirectory.get()
val springdocOpenapiVersion = "2.8.13"
val hmppsSqsVersion = "5.4.11"
val awaitilityVersion = "4.3.0"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$hmppsSqsVersion")

  implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.springframework.data:spring-data-envers")

  // Test dependencies
  testImplementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.7.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.34") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:localstack:$testContainersVersion")

  // Test fixtures dependencies
  testFixturesImplementation("org.assertj:assertj-core")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    /* added as a result of this:
    https://youtrack.jetbrains.com/issue/KT-73255
     */
    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=first-only")
  }
}

/*
  `dps-gradle-spring-boot` disabled the jar task in jira DT-2070, specifically to prevent the generation of the
  Spring Boot plain jar. The reason was that the `Dockerfile` copies the Spring Boot fat jar with:
  `COPY --from=builder --chown=appuser:appgroup /app/build/libs/hmpps-support-additional-needs-api*.jar /app/app.jar`
  The fat jar includes the date in the filename, hence needing to use a wildcard. Using the wildcard causes problems
  if there are multiple matching files (eg: the plain jar)

  The plugin `java-test-fixtures` requires the plain jar in order to access the compiled classes from the `main` source
  root. The `jar` task has been re-enabled here to allow `java-test-fixtures` to see the `main` classes, and a hook
  has been added to the `assemble` task to remove the plain jar and test-fixtures jar before assembling the Spring Boot
  fat jar.
 */
tasks.named("jar") {
  enabled = true
}

val test by testing.suites.existing(JvmTestSuite::class)

tasks.register<Test>("initialiseDatabase") {
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  include("**/InitialiseDatabase.class")
  onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
}

tasks.named("assemble") {
  // `assemble` task assembles the classes and dependencies into a fat jar
  // Beforehand we need to remove the plain jar and test-fixtures jars if they exist
  doFirst {
    delete(
      fileTree(buildDirectory)
        .include("libs/*-plain.jar")
        .include("libs/*-test-fixtures.jar"),
    )
  }
}

tasks.register<GenerateTask>("buildSupportAdditionalNeedsModel") {
  validateSpec.set(true)
  generatorName.set("kotlin-spring")
  templateDir.set("$projectDir/src/main/resources/static/openapi/templates")
  inputSpec.set("$projectDir/src/main/resources/static/openapi/SupportAdditionalNeedsAPI.yml")
  outputDir.set("$buildDirectory/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
      "useBeanValidation" to "true",
      "useSpringBoot3" to "true",
      "enumPropertyNaming" to "UPPERCASE",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "",
    ),
  )
}

tasks {
  withType<KtLintCheckTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildSupportAdditionalNeedsModel")
  }
  withType<KtLintFormatTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildSupportAdditionalNeedsModel")
  }
}

tasks.named("compileKotlin") {
  dependsOn("buildSupportAdditionalNeedsModel")
}

kotlin {
  kotlinDaemonJvmArgs = listOf("-Xmx1g")
  sourceSets["main"].apply {
    kotlin.srcDir("$buildDirectory/generated/src/main/kotlin")
  }
}

// Exclude generated code from linting
ktlint {
  filter {
    exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated/") }
  }
}
