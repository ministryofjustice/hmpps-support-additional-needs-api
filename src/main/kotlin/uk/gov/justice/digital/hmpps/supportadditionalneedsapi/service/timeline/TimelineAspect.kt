package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.TimelineRepository
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Aspect
@Component
class TimelineAspect(
  private val timelineRepository: TimelineRepository,
) {

  @AfterReturning("@annotation(timelineEvent)", returning = "result")
  fun recordTimelineEvents(joinPoint: JoinPoint, timelineEvent: TimelineEvent, result: Any?) {
    val methodSignature = joinPoint.signature as MethodSignature
    val paramNames = methodSignature.method.parameters.map { it.name }
    val args = joinPoint.args
    val argMap = paramNames.zip(args).toMap()

    val prisonNumber = resolvePrisonNumber(argMap, timelineEvent)
      ?: throw IllegalArgumentException("Missing prisonNumber")

    val itemList = resolveItemList(argMap)
      ?: throw IllegalArgumentException("Could not extract timeline-relevant item(s)")

    if (itemList.isEmpty()) return

    val prisonCode = resolvePrisonCode(argMap, itemList.first())

    val entries = itemList.map { item ->
      val additionalInfo = resolveAdditionalInfo(item, timelineEvent)
      TimelineEntity(
        id = UUID.randomUUID(),
        prisonNumber = prisonNumber,
        event = timelineEvent.eventType,
        additionalInfo = additionalInfo,
        createdAtPrison = prisonCode,
      )
    }

    timelineRepository.saveAll(entries)
  }

  private fun resolvePrisonNumber(argMap: Map<String, Any?>, timelineEvent: TimelineEvent): String? = argMap[timelineEvent.prisonNumberParam] as? String

  private fun resolvePrisonCode(argMap: Map<String, Any?>, fallbackItem: Any): String = argMap["prisonId"] as? String
    ?: extractField(fallbackItem, "prisonId")
    ?: "N/A"

  private fun resolveAdditionalInfo(item: Any, timelineEvent: TimelineEvent): String = extractField(item, timelineEvent.additionalInfoField)
    ?.let { "${timelineEvent.additionalInfoPrefix}$it" }
    ?: "UNKNOWN_TYPE"

  private fun resolveItemList(argMap: Map<String, Any?>): List<Any>? {
    // Case 1: a raw list (e.g. List<ChallengeRequest>)
    val rawList = argMap.values.firstOrNull { it is List<*> } as? List<*>
    if (rawList != null && rawList.all { it != null }) {
      return rawList.filterNotNull()
    }

    // Case 2: wrapped request object with (e.g. CreateChallengesRequest)
    val wrapper = argMap.values.firstOrNull { it != null && it !is String }
    val listFromWrapper = wrapper?.let { extractListProperty(it) }
    if (!listFromWrapper.isNullOrEmpty()) return listFromWrapper

    // Case 3: a single object (e.g. EducationALNAssessmentUpdateAdditionalInformation)
    val single = argMap.values.firstOrNull { it != null && it !is String }
    if (single != null) return listOf(single)

    return null
  }

  private fun extractListProperty(obj: Any): List<Any>? {
    val listProperty = obj::class.memberProperties.firstOrNull {
      it.returnType.toString().contains("List")
    } ?: return null

    listProperty.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return listProperty.getter.call(obj) as? List<Any>
  }

  private fun extractField(instance: Any, fieldName: String): String? = try {
    val property = instance::class.memberProperties.firstOrNull {
      it.name.equals(fieldName, ignoreCase = true)
    }
    property?.let {
      it.isAccessible = true
      it.getter.call(instance)?.toString()
    }
  } catch (e: Exception) {
    null
  }
}
