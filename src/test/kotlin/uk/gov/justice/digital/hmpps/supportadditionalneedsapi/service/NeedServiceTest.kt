package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ALNAssessmentMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.InstantMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.Instant
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

  @Mock
  private lateinit var alnAssessmentMapper: ALNAssessmentMapper

  @InjectMocks
  private lateinit var needService: NeedService
  private val curiousRef = UUID.randomUUID()
  private val curiousRef2 = UUID.randomUUID()

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
  fun `hasALNAssessmentNeed returns true if latest ALN assessment has need`() {
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

    assertTrue(needService.hasALNAssessmentNeed(prisonNumber))
  }

  @Test
  fun `hasALNAssessmentNeed returns null if no ALN assessment found`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)

    assertFalse(needService.hasALNAssessmentNeed(prisonNumber))
  }

  @Test
  fun `hasLDDNeed returns true if latest LDD assessment has need`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true))

    assertTrue(needService.hasLDDNeed(prisonNumber))
  }

  @Test
  fun `hasLDDNeed returns false if no LDD assessment found`() {
    val prisonNumber = randomValidPrisonNumber()
    whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)

    assertFalse(needService.hasLDDNeed(prisonNumber))
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

  private fun mockHasSanNeed(prisonNumber: String) {
    lenient().whenever(challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber)).thenReturn(true)
    lenient().whenever(conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).thenReturn(true)
  }

  private fun mockHasAlnNeed(prisonNumber: String) {
    lenient().whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(
        AlnAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = true,
          curiousReference = curiousRef,
          screeningDate = LocalDate.now(),
        ),
      )
  }

  private fun mockHasNoAlnNeed(prisonNumber: String) {
    lenient().whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(
        AlnAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = false,
          curiousReference = curiousRef,
          screeningDate = LocalDate.now(),
        ),
      )
  }

  private fun mockHasNoAlnScreener(prisonNumber: String) {
    lenient().whenever(alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(null)
  }

  private fun mockHasLddNeed(prisonNumber: String) {
    lenient().whenever(lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .thenReturn(LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true))
  }

  @Test
  fun `Case 1, hasNeed returns true if has SAN need, has ALN need and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasAlnNeed(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 2, hasNeed returns true if has SAN need, has ALN need and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasAlnNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 3, hasNeed returns true if has SAN need, doesn't have ALN need and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasNoAlnNeed(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 4, hasNeed returns true if has SAN need, doesn't have ALN need and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasNoAlnNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 5, hasNeed returns true if has SAN need, doesn't have ALN screener and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasNoAlnScreener(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 6, hasNeed returns true if has SAN need, doesn't have ALN screener and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasSanNeed(prisonNumber)
    mockHasNoAlnScreener(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 7, hasNeed returns true if doesn't have SAN need, has ALN need and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasAlnNeed(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 8, hasNeed returns true if doesn't have SAN need, has ALN need and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasAlnNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  // This test deviates from the expected logic. hasNeed should return false in this case.
  // This will be remediated in ESWE-2412.
  @Test
  fun `Case 9, hasNeed returns true if doesn't have SAN need, doesn't have ALN need and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasNoAlnNeed(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 10, hasNeed returns false if doesn't have SAN need, doesn't have ALN need and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasNoAlnNeed(prisonNumber)

    assertFalse(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 11, hasNeed returns true if doesn't have SAN need, doesn't have ALN screener and has LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasNoAlnScreener(prisonNumber)
    mockHasLddNeed(prisonNumber)

    assertTrue(needService.hasNeed(prisonNumber))
  }

  @Test
  fun `Case 12, hasNeed returns false if doesn't have SAN need, doesn't have ALN screener and doesn't have LDD need`() {
    val prisonNumber = randomValidPrisonNumber()

    mockHasNoAlnScreener(prisonNumber)

    assertFalse(needService.hasNeed(prisonNumber))
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
      .thenReturn(listOf(getChallengeEntity(prisonNumber)))
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
      .thenReturn(listOf(getConditionEntity(prisonNumber).copy(source = Source.CONFIRMED_DIAGNOSIS)))

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
      .thenReturn(listOf(getConditionEntity(prisonNumber).copy(source = Source.SELF_DECLARED)))

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

  @Test
  fun `getAlnScreenerNeeds returns assessment list in reverse chronological order, if ALN assessments exist`() {
    val prisonNumber = randomValidPrisonNumber()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    // ALN exists
    whenever(alnAssessmentRepository.findAllByPrisonNumber(prisonNumber)).thenReturn(
      listOf(
        makeAlnAssessmentEntity(prisonNumber, hasNeed = true, curiousReference = curiousRef, screeningDate = yesterday),
        makeAlnAssessmentEntity(prisonNumber, hasNeed = false, curiousReference = curiousRef2, screeningDate = today),
      ),
    )
    // Test with actual mapper
    val actualMapper = ALNAssessmentMapper(InstantMapper())
    whenever(alnAssessmentMapper.toModel(any<AlnAssessmentEntity>()))
      .thenAnswer { actualMapper.toModel(it.arguments[0] as AlnAssessmentEntity) }

    val result = needService.getAlnScreenerNeeds(prisonNumber)
    with(result) {
      assertThat(assessments).hasSize(2)
      assertThat(assessments[0])
        .hasHasNeed(false)
        .hasScreeningDate(today)
        .hasCuriousReference(curiousRef2)
      assertThat(assessments[1])
        .hasHasNeed(true)
        .hasScreeningDate(yesterday)
        .hasCuriousReference(curiousRef)
    }
  }

  private fun getChallengeEntity(
    prisonNumber: String,
  ): ChallengeEntity = ChallengeEntity(
    prisonNumber = prisonNumber,
    challengeType = getChallengeType(),
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    active = true,
    alnScreenerId = null,
  )

  private fun getConditionEntity(prisonNumber: String): ConditionEntity = ConditionEntity(
    prisonNumber = prisonNumber,
    conditionType = getConditionType(),
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    active = true,
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

  private fun makeAlnAssessmentEntity(
    prisonNumber: String,
    hasNeed: Boolean = false,
    screeningDate: LocalDate,
    curiousReference: UUID? = null,
    createdBy: String? = "tester",
    createdAt: Instant? = Instant.now(),
    updatedBy: String? = "tester",
    updatedAt: Instant? = Instant.now(),
  ) = AlnAssessmentEntity(prisonNumber, hasNeed, screeningDate, curiousReference).apply {
    this.createdBy = createdBy
    this.createdAt = createdAt
    this.updatedBy = updatedBy
    this.updatedAt = updatedAt
  }
}
