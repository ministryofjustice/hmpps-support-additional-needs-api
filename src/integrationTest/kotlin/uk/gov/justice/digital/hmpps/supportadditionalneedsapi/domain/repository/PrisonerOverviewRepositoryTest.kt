package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate

class PrisonerOverviewRepositoryTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonerOverviewRepository: PrisonerOverviewRepository

  private val today = LocalDate.now()

  @Nested
  inner class Education {
    @Test
    fun `should be in education when course started in the past and has no end date`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today.minusDays(10))

      assertThat(inEducation(prisonNumber)).isTrue()
    }

    @Test
    fun `should be in education when course starts today and has no end date`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today)

      assertThat(inEducation(prisonNumber)).isTrue()
    }

    // Courses are only in progress in SAN when there is no end date. Having ANY
    // end date means the course is complete.
    @Test
    fun `should not be in education when course starts today and has future end date`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today, endDate = today.plusDays(10))

      assertThat(inEducation(prisonNumber)).isFalse()
    }

    @Test
    fun `should not be in education when only course has ended`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today.minusDays(30), endDate = today.minusDays(1))

      assertThat(inEducation(prisonNumber)).isFalse()
    }

    @Test
    fun `should not be in education when course starts in the future`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today.plusDays(1))

      assertThat(inEducation(prisonNumber)).isFalse()
    }

    @Test
    fun `should be in education when there is an ended course and a current course`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today.minusDays(60), endDate = today.minusDays(30), qualificationCode = "OLD")
      prisonerInEducation(prisonNumber, learningStartDate = today.minusDays(5), qualificationCode = "NEW")

      assertThat(inEducation(prisonNumber)).isTrue()
    }

    // The result of this test does not align with business rules and will be fixed in ESWE-2437.
    @Test
    fun `should be in education for non PES course`() {
      val prisonNumber = randomValidPrisonNumber()
      prisonerInEducation(prisonNumber, learningStartDate = today.minusDays(1), fundingType = "DPS")

      assertThat(inEducation(prisonNumber)).isTrue()
    }

    @Test
    fun `should not be in education when prisoner has other data but no course`() {
      val prisonNumber = randomValidPrisonNumber()
      aValidConditionExists(prisonNumber)

      assertThat(inEducation(prisonNumber)).isFalse()
    }

    @Test
    fun `should return correct education status for each prisoner when querying multiple prisoners`() {
      val inEducationPrisoner = randomValidPrisonNumber()
      val endedEducationPrisoner = randomValidPrisonNumber()
      val futureEducationPrisoner = randomValidPrisonNumber()
      prisonerInEducation(inEducationPrisoner, learningStartDate = today.minusDays(1))
      prisonerInEducation(endedEducationPrisoner, learningStartDate = today.minusDays(10), endDate = today.minusDays(2))
      prisonerInEducation(futureEducationPrisoner, learningStartDate = today.plusDays(7))

      val overviews = prisonerOverviewRepository.findAllByPrisonNumbers(
        arrayOf(inEducationPrisoner, endedEducationPrisoner, futureEducationPrisoner),
      )

      assertThat(overviews.associate { it.prisonNumber to it.inEducation }).isEqualTo(
        mapOf(
          inEducationPrisoner to true,
          endedEducationPrisoner to false,
          futureEducationPrisoner to false,
        ),
      )
    }

    private fun inEducation(prisonNumber: String): Boolean {
      val overview = prisonerOverviewRepository.findOneByPrisonNumber(prisonNumber)
      assertThat(overview).isNotNull
      return overview!!.inEducation
    }
  }
}
