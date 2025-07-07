package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Table(name = "plan_creation_schedule")
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Audited(withModifiedFlag = false)
data class PlanCreationScheduleEntity(

  @Column(updatable = false)
  val prisonNumber: String,

  @Column
  var deadlineDate: LocalDate?,

  @Column
  @Enumerated(value = EnumType.STRING)
  var status: PlanCreationScheduleStatus,

  @Column
  var exemptionReason: String? = null,

  @Column
  var exemptionDetail: String? = null,

  @Column
  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var needSources: Set<NeedSource> = emptySet(),

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @Version
  @Column(name = "version")
  var version: Int? = null,

  @Id
  @Column
  val id: UUID = UUID.randomUUID(),

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @CreatedBy
  @Column(updatable = false)
  var createdBy: String? = null,

  @CreationTimestamp
  @Column(updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedBy
  @Column
  var updatedBy: String? = null,

  @UpdateTimestamp
  @Column
  var updatedAt: Instant? = null,
)

enum class PlanCreationScheduleStatus(val activeReview: Boolean) {
  SCHEDULED(true),
  EXEMPT_SYSTEM_TECHNICAL_ISSUE(true),
  EXEMPT_PRISONER_TRANSFER(false),
  EXEMPT_PRISONER_RELEASE(false),
  EXEMPT_PRISONER_DEATH(false),
  EXEMPT_PRISONER_MERGE(false),
  EXEMPT_PRISONER_NOT_COMPLY(false),
  EXEMPT_NOT_IN_EDUCATION(false),
  EXEMPT_NO_NEED(false),
  EXEMPT_UNKNOWN(false),
  COMPLETED(false),
}

enum class NeedSource {
  LDD_SCREENER,
  ALN_SCREENER,
  CONDITION_SELF_DECLARED,
  CONDITION_CONFIRMED_DIAGNOSIS,
  CHALLENGE_NOT_ALN_SCREENER,
}

@Converter
class NeedSourceConverter : AttributeConverter<Set<NeedSource>, String> {

  override fun convertToDatabaseColumn(attribute: Set<NeedSource>?): String? = attribute?.joinToString(",") { it.name }

  override fun convertToEntityAttribute(dbData: String?): Set<NeedSource> = dbData
    ?.split(",")
    ?.mapNotNull { it.trim().takeIf { it.isNotEmpty() }?.let(NeedSource::valueOf) }
    ?.toSet()
    ?: emptySet()
}
