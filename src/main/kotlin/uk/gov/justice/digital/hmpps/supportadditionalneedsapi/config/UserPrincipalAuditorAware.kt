package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class UserPrincipalAuditorAware : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = Optional.of(
    SecurityContextHolder.getContext()?.authentication?.principal?.let {
      it.toString()
    } ?: AuthAwareTokenConverter.SYSTEM_USER,
  )
}
