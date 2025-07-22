package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReferenceData
import java.time.LocalDateTime
import java.util.UUID

@Immutable
@Entity
@Table(name = "reference_data")
class ReferenceDataEntity(
  @Embedded
  val key: ReferenceDataKey,
  val description: String,
  val categoryListSequence: Int = 0,
  val listSequence: Int = 0,
  val deactivatedAt: LocalDateTime? = null,
  val categoryCode: String? = null,
  val categoryDescription: String? = null,
  val areaCode: String? = null,
  val areaDescription: String? = null,
  val defaultForCategory: Boolean = false,
  val screenerOption: Boolean = false,
  @Id
  @Column(name = "id")
  val id: UUID = UUID.randomUUID(),
) : ReferenceDataLookup by key {
  fun isActive() = deactivatedAt?.isBefore(LocalDateTime.now()) != true

  fun toModel() = ReferenceData(
    code,
    description,
    categoryCode,
    categoryDescription,
    areaCode,
    areaDescription,
    listSequence,
    isActive(),
  )
}

interface ReferenceDataLookup {
  val domain: Domain
  val code: String
}

@Embeddable
data class ReferenceDataKey(
  @Enumerated(EnumType.STRING)
  override val domain: Domain,
  override val code: String,
) : ReferenceDataLookup

enum class Domain {
  CONDITION,
  CHALLENGE,
  STRENGTH,
  SUPPORT_STRATEGY,
}
