package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayConfig {

  companion object {
    private val log = LoggerFactory.getLogger(FlywayConfig::class.java)
  }

  @Bean
  @DependsOn("primaryDataSource")
  fun flyway(
    @Qualifier("primaryDataSource") primaryDataSource: DataSource,
    @Value("\${spring.flyway.locations:classpath:migration}") locations: List<String>,
    @Value("\${spring.flyway.validateMigrationNaming:true}") validateMigrationNaming: Boolean,
    @Value("#{'\${spring.flyway.placeholders.dpr_user:dpr_user}'}") dprUser: String,
    @Value("#{'\${spring.flyway.placeholders.dpr_password:dpr_password}'}") dprPassword: String,
  ): Flyway {
    log.info("Configuring Flyway with locations: $locations")

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

    log.info("Running Flyway migration on primary database...")
    val result = flyway.migrate()
    log.info("Flyway migration completed. Migrations executed: ${result.migrationsExecuted}")

    return flyway
  }
}
