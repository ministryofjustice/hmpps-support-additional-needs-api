package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Configuration
@EnableCaching
class CacheConfiguration {

  companion object {
    const val USER_DETAILS = "userDetails"
    const val TTL_HOURS_USER_DETAILS: Long = 24
    const val BANK_HOLIDAYS: String = "bankHolidays"
    const val TTL_HOURS_BANK_HOLIDAYS: Long = 24
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(USER_DETAILS, BANK_HOLIDAYS)

  @CacheEvict(value = [USER_DETAILS])
  @Scheduled(fixedDelay = TTL_HOURS_USER_DETAILS, timeUnit = TimeUnit.HOURS)
  fun cacheEvictUserDetails() {
    log.info("Evicting cache: $USER_DETAILS after $TTL_HOURS_USER_DETAILS hours")
  }

  @CacheEvict(value = [BANK_HOLIDAYS])
  @Scheduled(fixedDelay = TTL_HOURS_BANK_HOLIDAYS, timeUnit = TimeUnit.HOURS)
  fun cacheEvictBankHolidays() {
    log.info("Evicting cache: $BANK_HOLIDAYS after $TTL_HOURS_BANK_HOLIDAYS hours")
  }
}
