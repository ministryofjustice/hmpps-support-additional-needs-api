package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/plan-creation-schedule")
@ConditionalOnProperty(name = ["ENABLE_TEST_ENDPOINTS"], havingValue = "true", matchIfMissing = false)
class TestDataController(private val planCreationScheduleRepository: PlanCreationScheduleRepository) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createPlanCreationSchedule(
    @PathVariable prisonNumber: String,
    @RequestBody request: Request,
  ): PlanCreationScheduleEntity = planCreationScheduleRepository.save(
    PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      deadlineDate = request.deadlineDate,
      status = PlanCreationScheduleStatus.SCHEDULED,
      createdAtPrison = request.prisonId,
      updatedAtPrison = request.prisonId,
    ),
  )
}

data class Request(val deadlineDate: LocalDate = LocalDate.now().plusDays(10), val prisonId: String = "BXI")
