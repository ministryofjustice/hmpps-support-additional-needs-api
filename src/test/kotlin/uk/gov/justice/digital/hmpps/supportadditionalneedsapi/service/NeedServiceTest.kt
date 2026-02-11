package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.LddAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.LddAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NeedServiceTest {

  @Mock
  private lateinit var challengeRepository: ChallengeRepository

  @Mock
  private lateinit var conditionRepository: ConditionRepository

  @Mock
  private lateinit var lddAssessmentRepository: LddAssessmentRepository

  @Mock
  private lateinit var alnAssessmentRepository: AlnAssessmentRepository

  @InjectMocks
  private lateinit var needService: NeedService
  val curiousRef = UUID.randomUUID()

  @Test
  fun `recordAlnScreenerNeed saves ALN assessment`() {
    val prisonNumber = randomValidPrisonNumber()
    val hasNeed = true

    needService.recordAlnScreenerNeed(prisonNumber, hasNeed, curiousRef, LocalDate.now())

    verify(alnAssessmentRepository).save(
      any<AlnAssessmentEntity>(),
    )
  }

  @Test
  fun `recordLddScreenerNeed saves LDD assessment`() {
    val prisonNumber = randomValidPrisonNumber()
    val hasNeed = false

    needService.recordLddScreenerNeed(prisonNumber, hasNeed)

    verify(lddAssessmentRepository).save(
      any<LddAssessmentEntity>(),
    )
  }

  @Test
  fun `hasALNScreenerNeed returns true if latest ALN assessment has need`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(
        AlnAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = true,
          curiousReference = curiousRef,
          screeningDate = LocalDate.now(),
        ),
      )

    assertTrue(needService.hasALNScreenerNeed(prisonNumber) == true)
  }

  @Test
  fun `hasALNScreenerNeed returns false if no ALN assessment found`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)

    assertNull(needService.hasALNScreenerNeed(prisonNumber))
  }

  @Test
  fun `hasLDDNeed returns true if latest LDD assessment has need`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true))

    assertTrue(needService.hasLDDNeed(prisonNumber) == true)
  }

  @Test
  fun `hasLDDNeed returns false if no LDD assessment found`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)

    assertNull(needService.hasLDDNeed(prisonNumber))
  }

  @Test
  fun `hasSANNeed returns true if any active non ALN challenge or condition exists`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber)).thenReturn(true)

    assertTrue(needService.hasActiveSANNeed(prisonNumber))
  }

  @Test
  fun `hasSANNeed returns false if no active challenges or conditions exist`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber)).thenReturn(false)
    whenever(conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).thenReturn(false)

    assertFalse(needService.hasActiveSANNeed(prisonNumber))
  }

  @Test
  fun `hasNeed returns false if need is an ALN challenge`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber))
      .thenReturn(
        false,
      )

    assertFalse(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `hasNeed returns true if need is not an ALN challenge`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber))
      .thenReturn(
        true,
      )

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `hasNeed returns true if ALN assessment has need and ignores LDD`() {
    val prisonNumber = randomValidPrisonNumber()

    // ALN returns true
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(
        AlnAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = true,
          curiousReference = curiousRef,
          screeningDate = LocalDate.now(),
        ),
      )

    // No active SAN needs
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber))
      .thenReturn(
        false,
      )
    whenever(conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).thenReturn(false)
    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `hasNeed returns false if ALN assessment exists but has no need and ignores LDD`() {
    val prisonNumber = randomValidPrisonNumber()

    // ALN returns true
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(
        AlnAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = false,
          curiousReference = curiousRef,
          screeningDate = LocalDate.now(),
        ),
      )

    // No active SAN needs
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber))
      .thenReturn(
        false,
      )
    whenever(conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).thenReturn(false)
    assertFalse(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `hasNeed returns true if ALN assessment is null has has LDD with need`() {
    val prisonNumber = randomValidPrisonNumber()

    // ALN returns null
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)

    // LDD returns true,
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true))

    // No active SAN needs
    whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber))
      .thenReturn(
        false,
      )
    whenever(conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).thenReturn(false)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `getNeedSources returns ALN_SCREENER when ALN has need`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(AlnAssessmentEntity(prisonNumber, true, LocalDate.now(), curiousRef))
    // No active SAN needs
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.contains(NeedSource.ALN_SCREENER))
    assertFalse(result.contains(NeedSource.LDD_SCREENER))
  }

  @Test
  fun `getNeedSources returns LDD_SCREENER when ALN is null and LDD has need`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(LddAssessmentEntity(prisonNumber, true))
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.contains(NeedSource.LDD_SCREENER))
    assertFalse(result.contains(NeedSource.ALN_SCREENER))
  }

  @Test
  fun `getNeedSources returns CHALLENGE_NOT_ALN_SCREENER when active non-screener challenge exists`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber))
      .thenReturn(listOf(getChallengeEntity(prisonNumber, active = true, alnScreener = false)))
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.contains(NeedSource.CHALLENGE_NOT_ALN_SCREENER))
  }

  @Test
  fun `getNeedSources returns CONDITION_CONFIRMED_DIAGNOSIS when active confirmed diagnosis condition exists`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber))
      .thenReturn(listOf(getConditionEntity(prisonNumber, active = true).copy(source = Source.CONFIRMED_DIAGNOSIS)))

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.contains(NeedSource.CONDITION_CONFIRMED_DIAGNOSIS))
  }

  @Test
  fun `getNeedSources returns CONDITION_SELF_DECLARED when active self-declared condition exists`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber))
      .thenReturn(listOf(getConditionEntity(prisonNumber, active = true).copy(source = Source.SELF_DECLARED)))

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.contains(NeedSource.CONDITION_SELF_DECLARED))
  }

  @Test
  fun `getNeedSources returns empty set when no sources are present`() {
    val prisonNumber = randomValidPrisonNumber()

    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).thenReturn(null)
    whenever(challengeRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())
    whenever(conditionRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(emptyList())

    val result = needService.getNeedSources(prisonNumber)
    assertTrue(result.isEmpty())
  }

  private fun getChallengeEntity(
    prisonNumber: String,
    active: Boolean = true,
    alnScreener: Boolean = true,
  ): ChallengeEntity = ChallengeEntity(
    prisonNumber = prisonNumber,
    challengeType = getChallengeType(),
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    active = active,
    alnScreenerId = if (alnScreener) UUID.randomUUID() else null,
  )

  private fun getConditionEntity(prisonNumber: String, active: Boolean = true): ConditionEntity = ConditionEntity(
    prisonNumber = prisonNumber,
    conditionType = getConditionType(),
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    active = active,
    source = Source.SELF_DECLARED,
  )

  private fun getChallengeType(): ReferenceDataEntity = ReferenceDataEntity(
    ReferenceDataKey(Domain.CHALLENGE, "reading"),
    description = "struggles to read",
  )

  private fun getConditionType(): ReferenceDataEntity = ReferenceDataEntity(
    ReferenceDataKey(Domain.CONDITION, "ADHD"),
    description = "ADHD",
  )
}
