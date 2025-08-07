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
    val method = methodSignature.method
    val paramNames = method.parameters.map { it.name }
    val args = joinPoint.args
    val argMap = paramNames.zip(args).toMap()

    val prisonNumber = argMap[timelineEvent.prisonNumberParam] as? String
      ?: throw IllegalArgumentException("Missing prisonNumber")

    val itemList = findItemList(argMap)
      ?: throw IllegalArgumentException("Could not extract list of items")

    if (itemList.isEmpty()) return

    // Try to extract prisonId from the method args (if present)
    val prisonCode = argMap["prisonId"] as? String
      ?: extractField(itemList.first(), "prisonId")
      ?: "UNK"

    val timelineEntries = itemList.map { item ->
      val additionalInfo = extractField(item, timelineEvent.additionalInfoField)
        ?.let { "${timelineEvent.additionalInfoPrefix}$it" }
        ?: "UNKNOWN_TYPE"

      TimelineEntity(
        id = UUID.randomUUID(),
        prisonNumber = prisonNumber,
        event = timelineEvent.eventType,
        additionalInfo = additionalInfo,
        createdAtPrison = prisonCode,
      )
    }

    timelineRepository.saveAll(timelineEntries)
  }

  private fun findItemList(argMap: Map<String, Any?>): List<Any>? {
    // Look for a raw list argument directly
    val rawList = argMap.values.firstOrNull { it is List<*> } as? List<*>
    if (rawList != null && rawList.all { it != null }) {
      return rawList.filterNotNull()
    }

    // Otherwise, try to extract from a property on a request object
    val requestObject = argMap.values.firstOrNull { it != null && it !is String }
    return requestObject?.let { extractRequestItems(it) }
  }

  private fun extractRequestItems(request: Any): List<Any>? {
    val property = request::class.memberProperties.firstOrNull {
      it.returnType.toString().contains("List")
    } ?: return null

    property.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return property.getter.call(request) as? List<Any>
  }

  private fun extractField(instance: Any, fieldName: String): String? = try {
    val property = instance::class.memberProperties.firstOrNull {
      it.name.equals(fieldName, ignoreCase = true)
    }
    property?.let {
      it.isAccessible = true
      it.getter.call(instance) as? String
    }
  } catch (e: Exception) {
    null
  }
}
