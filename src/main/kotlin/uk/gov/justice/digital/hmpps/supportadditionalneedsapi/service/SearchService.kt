package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PrisonerOverviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PrisonerOverviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SentenceTypeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Person
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortDirection
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortField
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.LocalDate

@Service
class SearchService(
  private val prisonerSearchApiService: PrisonerSearchApiService,
  private val prisonerOverviewRepository: PrisonerOverviewRepository,
  private val workingDayService: WorkingDayService,
) {
  fun searchPrisoners(searchCriteria: SearchCriteria): List<Person> {
    // Get all prisoners in the prison
    val prisonerSearchPrisoners = prisonerSearchApiService.getAllPrisonersInPrison(searchCriteria.prisonId)

    // Get all prisonNumbers from filtered list
    val prisonNumbers = prisonerSearchPrisoners.map { it.prisonerNumber }

    // Fetch overview data in chunks to avoid query length issues
    val prisonerOverviewResults = prisonNumbers.chunked(500).flatMap {
      prisonerOverviewRepository.findByPrisonNumberIn(it)
    }.associateBy { it.prisonNumber }

    // Map the final results
    val persons = prisonerSearchPrisoners.map { prisoner ->
      val overview = prisonerOverviewResults[prisoner.prisonerNumber]
      Person(
        forename = prisoner.firstName,
        surname = prisoner.lastName,
        prisonNumber = prisoner.prisonerNumber,
        dateOfBirth = prisoner.dateOfBirth,
        releaseDate = prisoner.releaseDate,
        cellLocation = prisoner.cellLocation,
        sentenceType = SentenceTypeMapper.fromPrisonerSearchApiToModel(prisoner.legalStatus),
        inEducation = overview?.inEducation ?: false,
        hasAdditionalNeed = overview?.hasNeed ?: false,
        deadlineDate = overview?.deadlineDate,
        planStatus = determinePlanStatus(overview),
      )
    }

    // Filter and sort them according to the search criteria
    return persons
      .filterByCriteria(searchCriteria)
      .sortBy(searchCriteria)
  }

  fun determinePlanStatus(overview: PrisonerOverviewEntity?): PlanStatus {
    val today = LocalDate.now()
    val todayPlus5WorkingDays = plus5WorkingDays()

    return when {
      // No overview record at all
      overview == null -> PlanStatus.NO_PLAN

      // No need, no education, and no plan = no plan required
      !overview.inEducation && !overview.hasNeed && !overview.hasPlan -> PlanStatus.NO_PLAN

      // Explicitly declined
      overview.planDeclined -> PlanStatus.PLAN_DECLINED

      // Overdue review
      overview.reviewDeadlineDate != null &&
        overview.deadlineDate != null &&
        overview.deadlineDate == overview.reviewDeadlineDate &&
        overview.reviewDeadlineDate < today -> PlanStatus.REVIEW_OVERDUE

      // Overdue plan creation
      overview.planCreationDeadlineDate != null &&
        overview.deadlineDate != null &&
        overview.deadlineDate == overview.planCreationDeadlineDate &&
        overview.planCreationDeadlineDate < today -> PlanStatus.PLAN_OVERDUE

      // Needs plan (has needs and education, no deadline, and no plan yet)
      !overview.hasPlan &&
        overview.deadlineDate == null &&
        overview.hasNeed &&
        overview.inEducation -> PlanStatus.NEEDS_PLAN

      // Review due soon (within 5 working days)
      overview.reviewDeadlineDate != null &&
        overview.deadlineDate != null &&
        overview.deadlineDate == overview.reviewDeadlineDate &&
        !overview.reviewDeadlineDate.isBefore(today) &&
        !overview.reviewDeadlineDate.isAfter(todayPlus5WorkingDays) -> PlanStatus.REVIEW_DUE

      // Plan is due soon (within 5 working days)
      overview.planCreationDeadlineDate != null &&
        overview.deadlineDate != null &&
        overview.deadlineDate == overview.planCreationDeadlineDate &&
        !overview.hasPlan &&
        !overview.planDeclined &&
        !overview.planCreationDeadlineDate.isBefore(today) &&
        !overview.planCreationDeadlineDate.isAfter(todayPlus5WorkingDays) -> PlanStatus.PLAN_DUE

      // Has a plan but is effectively inactive
      overview.hasPlan && (!overview.inEducation || !overview.hasNeed) -> PlanStatus.INACTIVE_PLAN

      // Has a plan and is active
      overview.hasPlan -> PlanStatus.ACTIVE_PLAN

      // Fallback
      else -> PlanStatus.NO_PLAN
    }
  }

  fun plus5WorkingDays(): LocalDate = workingDayService.getNextWorkingDayNDaysFromToday(5)
}

private fun List<Person>.filterByCriteria(searchCriteria: SearchCriteria): List<Person> = this.filter { prisoner ->
  // Filter by prisoner name or number
  (
    searchCriteria.prisonerNameOrNumber.isNullOrBlank() ||
      prisoner.forename.contains(searchCriteria.prisonerNameOrNumber, ignoreCase = true) ||
      prisoner.surname.contains(searchCriteria.prisonerNameOrNumber, ignoreCase = true) ||
      prisoner.prisonNumber.equals(searchCriteria.prisonerNameOrNumber, ignoreCase = true)
    )
}

val customPlanStatusOrder = listOf(
  PlanStatus.NEEDS_PLAN,
  PlanStatus.PLAN_DUE,
  PlanStatus.REVIEW_DUE,
  PlanStatus.ACTIVE_PLAN,
  PlanStatus.PLAN_OVERDUE,
  PlanStatus.REVIEW_OVERDUE,
  PlanStatus.INACTIVE_PLAN,
  PlanStatus.PLAN_DECLINED,
  PlanStatus.NO_PLAN,
).withIndex().associate { it.value to it.index }

private fun List<Person>.sortBy(searchCriteria: SearchCriteria): List<Person> {
  val comparator: Comparator<Person> = when (searchCriteria.sortBy) {
    SearchSortField.PRISONER_NAME -> compareBy { it.surname }
    SearchSortField.PRISON_NUMBER -> compareBy { it.prisonNumber }
    SearchSortField.RELEASE_DATE -> compareBy(nullsLast()) { it.releaseDate }
    SearchSortField.CELL_LOCATION -> compareBy(nullsLast()) { it.cellLocation }
    SearchSortField.DEADLINE_DATE -> compareBy(nullsLast()) { it.deadlineDate }
    SearchSortField.PLAN_STATUS -> compareBy { person -> customPlanStatusOrder[person.planStatus] }
  }

  return when (searchCriteria.sortDirection) {
    SearchSortDirection.ASC -> this.sortedWith(comparator)
    SearchSortDirection.DESC -> this.sortedWith(comparator.reversed())
  }
}

data class SearchCriteria(
  val prisonId: String,
  val prisonerNameOrNumber: String? = null,
  val sortBy: SearchSortField,
  val sortDirection: SearchSortDirection,
)
