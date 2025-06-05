package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.manageusers.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.manageusers.UserDetailsDto
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.AuthAwareTokenConverter
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.CacheConfiguration.Companion.USER_DETAILS

@Component
class ManageUserService(
  private val manageUsersApiClient: ManageUsersApiClient,
) {
  @Cacheable(USER_DETAILS)
  fun getUserDetails(username: String): UserDetailsDto = if (username == AuthAwareTokenConverter.SYSTEM_USER) {
    UserDetailsDto(username, true, username)
  } else {
    manageUsersApiClient.getUserDetails(username)
  }
}
