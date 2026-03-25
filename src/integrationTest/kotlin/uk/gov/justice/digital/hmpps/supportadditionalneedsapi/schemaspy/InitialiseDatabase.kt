package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.schemaspy

import mu.KotlinLogging
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase

class InitialiseDatabase : IntegrationTestBase() {
  private val log = KotlinLogging.logger {}

  // This is needed to initialise the database for schema spy
  @Test
  fun `initialises database`() {
    log.debug("Database has been initialised by IntegrationTestBase")
  }
}
