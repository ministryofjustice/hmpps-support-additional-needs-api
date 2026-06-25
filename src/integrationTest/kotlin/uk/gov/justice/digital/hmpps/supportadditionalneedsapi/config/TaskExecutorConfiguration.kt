package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * Configuration for a synchronous task executor to be used in integration tests.
 * This effectively disables async support for the integration tests, ensuring that tasks are executed immediately
 * in the calling thread. This is particularly useful for tests that use or assert timeline entries or SQS messages,
 * most of which the main implementation creates/triggers asynchronously.
 */
@TestConfiguration
class TaskExecutorConfiguration {
  @Bean
  @Primary
  fun taskExecutor(): TaskExecutor = SyncTaskExecutor()
}
