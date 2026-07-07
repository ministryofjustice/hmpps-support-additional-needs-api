package uk.gov.justice.digital.hmpps.supportadditionalneedsapi

import org.springframework.test.web.reactive.server.FluxExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse

fun WebTestClient.RequestHeadersSpec<*>.bearerToken(bearerToken: String): WebTestClient.RequestBodySpec = header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec

fun <T : Any> WebTestClient.RequestBodySpec.withBody(requestBody: T): WebTestClient.RequestBodySpec = body(
  Mono.just(requestBody),
  requestBody.javaClass,
) as RequestBodySpec

fun WebTestClient.ResponseSpec.returnError() = this.returnResult(ErrorResponse::class.java)

fun <T : Any> FluxExchangeResult<T>.body(): T = this.responseBody.blockFirst()!!
