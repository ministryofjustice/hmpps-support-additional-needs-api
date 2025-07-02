package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ReviewScheduleService
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
    @Valid
    @RequestBody request: Request,
  ): PlanCreationScheduleEntity =
    planCreationScheduleRepository.save(
      PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        deadlineDate = request.deadlineDate,
        status = PlanCreationScheduleStatus.SCHEDULED,
        createdAtPrison = prisonNumber,
        updatedAtPrison = prisonNumber,
      ),
    )
}

data class Request(val deadlineDate: LocalDate, val prisonNumber: String)
