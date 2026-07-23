package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidEducationDto
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidV2Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

class AdminToolsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}"
  }

  @Test
  fun `test ALN trigger`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    stubForBankHoliday()
    val prisonNumber = randomValidPrisonNumber()
    stubGetCurious2LearnerAssessments(prisonNumber, createTestALNAssessment(prisonNumber, assessmentDate = LocalDate.of(2025, 1, 28)))

    // When
    webTestClient.post()
      .uri("$URI_TEMPLATE/aln-trigger", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val alnAssessment = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      assertThat(alnAssessment!!.hasNeed).isTrue()
      assertThat(alnAssessment.screeningDate).isEqualTo(LocalDate.of(2025, 1, 28))
    } matches { it != null }

    assertThat(needService.hasALNAssessmentNeed(prisonNumber)).isTrue()
    assertThat(needService.hasNeed(prisonNumber)).isTrue()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_ASSESSMENT_TRIGGER)
    assertThat(timelineEntries[0].additionalInfo).contains("curiousReference:")
  }

  @Test
  fun `test Education trigger`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    stubForBankHoliday()
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          establishmentId = "CFI",
          establishmentName = "CARDIFF (HMP)",
          qualificationCode = "60322457",
          qualificationName = "Award in Cycle Maintenance",
          learningStartDate = LocalDate.parse("2025-10-02"),
          learningPlannedEndDate = LocalDate.parse("2025-01-31"),
        ),
      ),
    )

    // When
    webTestClient.post()
      .uri("$URI_TEMPLATE/education-trigger", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val isInEducation = educationService.hasActiveEducationEnrollment(prisonNumber)
      Assertions.assertThat(isInEducation).isTrue()
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(enrolments).hasSize(1)
    assertThat(enrolments[0].endDate).isNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
  }
}
