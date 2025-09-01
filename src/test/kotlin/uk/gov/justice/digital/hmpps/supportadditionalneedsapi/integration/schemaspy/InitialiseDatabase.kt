package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.schemaspy

import mu.KotlinLogging
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase

class InitialiseDatabase : IntegrationTestBase() {
  private val log = KotlinLogging.logger {}

  @Test
  fun `initialises database`() {
    log.debug("Database has been initialised by IntegrationTestBase")
  }
}
