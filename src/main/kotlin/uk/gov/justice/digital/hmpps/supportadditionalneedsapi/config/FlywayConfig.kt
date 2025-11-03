package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayConfig(
  @Qualifier("primaryDataSource") private val primaryDataSource: DataSource,
  @Value("\${spring.flyway.locations:classpath:migration}") private val locations: List<String>,
  @Value("\${spring.flyway.validateMigrationNaming:true}") private val validateMigrationNaming: Boolean,
  @Value("#{'\${spring.flyway.placeholders.dpr_user:dpr_user}'}") private val dprUser: String,
  @Value("#{'\${spring.flyway.placeholders.dpr_password:dpr_password}'}") private val dprPassword: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(FlywayConfig::class.java)
  }

  @PostConstruct
  fun migrateDatabase() {
    log.info("Starting Flyway migration process...")
    log.info("Flyway enabled: true")
    log.info("Flyway locations: $locations")

    try {
      // Test the datasource connection
      primaryDataSource.connection.use { conn ->
        log.info("Database connection successful: ${conn.metaData.url}")
        log.info("Database user: ${conn.metaData.userName}")
      }
    } catch (e: Exception) {
      log.error("Failed to connect to primary datasource for Flyway migration", e)
      throw RuntimeException("Failed to connect to database for Flyway migration", e)
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

    log.info("Executing Flyway migration on primary database...")
    try {
      val result = flyway.migrate()
      log.info("Flyway migration successful! Migrations executed: ${result.migrationsExecuted}")

      // Log the current schema version
      val info = flyway.info()
      val current = info.current()
      if (current != null) {
        log.info("Current schema version: ${current.version} - ${current.description}")
      }
      log.info("Flyway migration completed successfully")
    } catch (e: Exception) {
      log.error("Flyway migration failed!", e)
      throw RuntimeException("Flyway migration failed", e)
    }
  }
}
