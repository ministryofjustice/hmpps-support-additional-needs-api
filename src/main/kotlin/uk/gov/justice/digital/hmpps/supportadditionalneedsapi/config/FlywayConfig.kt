package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayConfig(
  @Qualifier("primaryDataSource") private val primaryDataSource: DataSource,
  @Value("\${spring.flyway.locations}") private val locations: List<String>,
  @Value("\${spring.flyway.validateMigrationNaming:true}") private val validateMigrationNaming: Boolean,
  @Value("#{'\${spring.flyway.placeholders.dpr_user:dpr_user}'}") private val dprUser: String,
  @Value("#{'\${spring.flyway.placeholders.dpr_password:dpr_password}'}") private val dprPassword: String,
) {

  @PostConstruct
  fun migratePrimaryDatabase() {
    val placeholders = mapOf(
      "dpr_user" to dprUser,
      "dpr_password" to dprPassword,
    )

    Flyway.configure()
      .dataSource(primaryDataSource)
      .locations(*locations.toTypedArray())
      .baselineOnMigrate(true)
      .validateMigrationNaming(validateMigrationNaming)
      .placeholders(placeholders)
      .load()
      .migrate()
  }
}
