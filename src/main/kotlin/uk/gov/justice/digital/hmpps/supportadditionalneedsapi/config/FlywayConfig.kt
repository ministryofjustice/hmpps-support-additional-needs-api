package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

@Configuration
class FlywayConfig {

  companion object {
    private val log = LoggerFactory.getLogger(FlywayConfig::class.java)
  }

  /**
   * Create Flyway bean with explicit migration via initMethod
   * This ensures proper bean initialization order
   */
  @Bean(initMethod = "migrate")
  @DependsOn("primaryDataSource")
  fun flyway(
    @Qualifier("primaryDataSource") primaryDataSource: DataSource,
    @Value("\${spring.flyway.enabled:true}") flywayEnabled: Boolean,
    @Value("\${spring.flyway.locations:classpath:migration}") locations: List<String>,
    @Value("\${spring.flyway.validateMigrationNaming:true}") validateMigrationNaming: Boolean,
    @Value("#{'\${spring.flyway.placeholders.dpr_user:dpr_user}'}") dprUser: String,
    @Value("#{'\${spring.flyway.placeholders.dpr_password:dpr_password}'}") dprPassword: String,
  ): Flyway {
    log.info("=== FlywayConfig: Creating Flyway bean - enabled: $flywayEnabled ===")
    log.info("Flyway locations: $locations")

    if (!flywayEnabled) {
      log.warn("Flyway is disabled - returning no-op Flyway instance")
      // Return a configured but non-migrating Flyway instance
      return Flyway.configure()
        .dataSource(primaryDataSource)
        .locations(*locations.toTypedArray())
        .skipExecutingMigrations(true)
        .load()
    }

    try {
      primaryDataSource.connection.use { conn ->
        log.info("Database connection successful: ${conn.metaData.url}")
      }
    } catch (e: Exception) {
      log.error("Failed to connect to primary datasource", e)
      throw RuntimeException("Failed to connect to database for Flyway", e)
    }

    val placeholders = mapOf(
      "dpr_user" to dprUser,
      "dpr_password" to dprPassword,
    )

    val flyway = Flyway.configure()
      .dataSource(primaryDataSource)
      .locations(*locations.toTypedArray())
      .baselineOnMigrate(true)
      .validateMigrationNaming(validateMigrationNaming)
      .placeholders(placeholders)
      .load()

    log.info("Flyway bean created - migration will run via initMethod")
    return flyway
  }

  /**
   * FlywayMigrationInitializer ensures EntityManagerFactory waits for Flyway
   * Critical for test environments where timing matters
   */
  @Bean
  fun flywayInitializer(flyway: Flyway): FlywayMigrationInitializer {
    log.info("=== Creating FlywayMigrationInitializer to ensure proper ordering ===")
    return FlywayMigrationInitializer(flyway)
  }
}
