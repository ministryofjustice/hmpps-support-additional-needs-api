package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.servlet.RequestDispatcher
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ElementKind
import mu.KotlinLogging
import org.hibernate.validator.internal.engine.path.PathImpl
import org.springframework.beans.TypeMismatchException
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.DuplicateChallengeException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.DuplicateConditionException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PersonAlreadyHasAPlanException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanCreationScheduleNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanCreationScheduleStateException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse

private val log = KotlinLogging.logger {}

/**
 * Global Exception Handler. Handles specific exceptions thrown by the application by returning a suitable [ErrorResponse]
 * response entity.
 *
 * Our standard pattern here is to return an [ErrorResponse]. Please think carefully about writing a response handler
 * method that does not follow this pattern. Please try not to use [handleExceptionInternal] as this returns a response
 * body of a simple string rather than a structured response body.
 *
 * Our preferred approach is to use the method [populateErrorResponseAndHandleExceptionInternal] which builds and returns
 * the [ErrorResponse] complete with correctly populated status code field. This method also populates the message field
 * of [ErrorResponse] from the exception. In the case that the exception message is not suitable for exposing through
 * the REST API, this can be overridden by manually setting the message on the request attribute. eg:
 *
 * ```
 *     request.setAttribute(ERROR_MESSAGE, "A simpler error message that does not expose internal detail", SCOPE_REQUEST)
 * ```
 *
 */
@RestControllerAdvice
class GlobalExceptionHandler(private val errorAttributes: ApiRequestErrorAttributes) : ResponseEntityExceptionHandler() {

  /**
   * Exception handler to return a 403 Forbidden ErrorResponse for an AccessDeniedException.
   */
  @ExceptionHandler(
    value = [
      AccessDeniedException::class,
    ],
  )
  protected fun handleAccessDeniedExceptionReturnForbiddenErrorResponse(
    e: RuntimeException,
    request: WebRequest,
  ): ResponseEntity<Any> {
    log.info("Access denied exception: {}", e.message)
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN.value(),
          userMessage = e.message,
          developerMessage = "Access denied on ${request.getDescription(false)}",
        ),
      )
  }

  /**
   * Exception handler to return a 400 Bad Request ErrorResponse, specifically for a ConstraintViolationException.
   *
   * This is because the message property of ConstraintViolationException does not contain sufficient/formatted details
   * as to the nature of the constraint violations. This handler constructs the error message from each violation in the
   * exception, before using it to create the ErrorResponse which is rendered through the REST API.
   */
  @ExceptionHandler(ConstraintViolationException::class)
  fun handleConstraintViolationException(
    e: ConstraintViolationException,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    val violations: Set<ConstraintViolation<*>> = e.constraintViolations
    val errorMessage = if (violations.isNotEmpty()) {
      violations.joinToString {
        if (it.relatesToNamedParameter()) {
          "${it.propertyPath} ${it.message}"
        } else {
          it.message
        }
      }
    } else {
      "Validation error"
    }
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, errorMessage, RequestAttributes.SCOPE_REQUEST)
    return populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.BAD_REQUEST, request)
  }

  /**
   * Exception handler to return a 503 Service Unavailable ErrorResponse, specifically for a DataAccessException.
   */
  @ExceptionHandler(DataAccessException::class)
  fun handleDataAccessException(
    e: DataAccessException,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    log.error("Unexpected database exception", e)
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Service unavailable", RequestAttributes.SCOPE_REQUEST)
    return populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.SERVICE_UNAVAILABLE, request)
  }

  /**
   * Overrides the MethodArgumentNotValidException exception handler to return a 400 Bad Request ErrorResponse
   */
  override fun handleMethodArgumentNotValid(
    e: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.BAD_REQUEST, request)

  /**
   * Overrides the HttpMessageNotReadableException exception handler to return a 400 Bad Request ErrorResponse
   */
  override fun handleHttpMessageNotReadable(
    e: HttpMessageNotReadableException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.BAD_REQUEST, request)

  override fun handleTypeMismatch(
    e: TypeMismatchException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest,
  ): ResponseEntity<Any>? = populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.BAD_REQUEST, request)

  @ExceptionHandler(Exception::class)
  fun unexpectedExceptionHandler(e: Exception, request: WebRequest): ResponseEntity<Any>? {
    log.error("Unexpected exception", e)
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Service unavailable", RequestAttributes.SCOPE_REQUEST)
    return populateErrorResponseAndHandleExceptionInternal(e, HttpStatus.INTERNAL_SERVER_ERROR, request)
  }

  /**
   * Exception handler to return a 404 Not Found ErrorResponse
   */
  @ExceptionHandler(
    value = [
      ConditionNotFoundException::class,
      ChallengeNotFoundException::class,
      PlanNotFoundException::class,
      PlanCreationScheduleNotFoundException::class,
    ],
  )
  fun handleExceptionReturnNotFoundErrorResponse(
    e: RuntimeException,
    request: WebRequest,
  ): ResponseEntity<ErrorResponse> {
    log.info("Not found exception: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND.value(),
          userMessage = e.message,
        ),
      )
  }

  /**
   * Exception handler to return a 409 conflict error.
   */
  @ExceptionHandler(
    value = [
      DuplicateConditionException::class,
      DuplicateChallengeException::class,
      PersonAlreadyHasAPlanException::class,
      PlanCreationScheduleStateException::class,
    ],
  )
  protected fun handleExceptionReturnConflictErrorResponse(
    e: RuntimeException,
    request: WebRequest,
  ): ResponseEntity<Any> {
    log.info("Conflict exception: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT.value(),
          userMessage = e.message,
        ),
      )
  }

  private fun populateErrorResponseAndHandleExceptionInternal(
    exception: Exception,
    status: HttpStatus,
    request: WebRequest,
  ): ResponseEntity<Any>? {
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status.value(), RequestAttributes.SCOPE_REQUEST)
    val body = errorAttributes.getErrorResponse(request)
    return handleExceptionInternal(exception, body, HttpHeaders(), status, request)
  }

  /**
   * Returns true is this [ConstraintViolation] relates to a named parameter such as a constraint annotation on a
   * property in the request body, or a constraint annotation on the method argument.
   * Knowing whether the constraint relates to a named parameter means we can use the name in the error response.
   */
  private fun ConstraintViolation<*>.relatesToNamedParameter(): Boolean = propertyPath is PathImpl &&
    when ((propertyPath as PathImpl).leafNode.kind) {
      ElementKind.PROPERTY, ElementKind.PARAMETER -> true
      else -> false
    }
}
