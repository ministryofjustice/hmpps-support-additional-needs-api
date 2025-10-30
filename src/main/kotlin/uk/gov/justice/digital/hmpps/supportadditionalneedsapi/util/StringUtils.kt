package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.util

object StringUtils {
  
  /**
   * Converts a camelCase or PascalCase string to snake_case.
   * 
   * Examples:
   * - "firstName" -> "first_name"
   * - "FirstName" -> "first_name"
   * - "APIResponse" -> "apiresponse"
   * 
   * @param input the string to convert
   * @return the snake_case version of the input string
   */
  fun toSnakeCase(input: String): String {
    return input.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
  }
}