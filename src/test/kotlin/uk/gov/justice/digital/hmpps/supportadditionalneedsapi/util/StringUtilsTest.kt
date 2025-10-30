package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringUtilsTest {

  @Test
  fun `toSnakeCase should convert camelCase to snake_case`() {
    assertThat(StringUtils.toSnakeCase("firstName")).isEqualTo("first_name")
    assertThat(StringUtils.toSnakeCase("lastName")).isEqualTo("last_name")
    assertThat(StringUtils.toSnakeCase("phoneNumber")).isEqualTo("phone_number")
  }

  @Test
  fun `toSnakeCase should convert PascalCase to snake_case`() {
    assertThat(StringUtils.toSnakeCase("FirstName")).isEqualTo("first_name")
    assertThat(StringUtils.toSnakeCase("LastName")).isEqualTo("last_name")
    assertThat(StringUtils.toSnakeCase("PhoneNumber")).isEqualTo("phone_number")
  }

  @Test
  fun `toSnakeCase should handle single word strings`() {
    assertThat(StringUtils.toSnakeCase("name")).isEqualTo("name")
    assertThat(StringUtils.toSnakeCase("Name")).isEqualTo("name")
    assertThat(StringUtils.toSnakeCase("NAME")).isEqualTo("name")
  }

  @Test
  fun `toSnakeCase should handle strings with numbers`() {
    assertThat(StringUtils.toSnakeCase("address1")).isEqualTo("address1")
    // Numbers followed by uppercase are not split by our simple regex
    assertThat(StringUtils.toSnakeCase("line2Address")).isEqualTo("line2address")
    assertThat(StringUtils.toSnakeCase("lineAddress2")).isEqualTo("line_address2")
  }

  @Test
  fun `toSnakeCase should handle consecutive uppercase letters`() {
    // Note: Consecutive uppercase letters are not split (limitation of simple regex)
    assertThat(StringUtils.toSnakeCase("APIResponse")).isEqualTo("apiresponse")
    assertThat(StringUtils.toSnakeCase("XMLHttpRequest")).isEqualTo("xmlhttp_request")
  }

  @Test
  fun `toSnakeCase should handle empty and blank strings`() {
    assertThat(StringUtils.toSnakeCase("")).isEqualTo("")
    assertThat(StringUtils.toSnakeCase(" ")).isEqualTo(" ")
  }

  @Test
  fun `toSnakeCase should handle strings already in snake_case`() {
    assertThat(StringUtils.toSnakeCase("first_name")).isEqualTo("first_name")
    assertThat(StringUtils.toSnakeCase("last_name")).isEqualTo("last_name")
  }

  @Test
  fun `toSnakeCase should handle mixed cases`() {
    // Our regex splits on lowercase followed by uppercase
    assertThat(StringUtils.toSnakeCase("getHTTPResponseCode")).isEqualTo("get_httpresponse_code")
    assertThat(StringUtils.toSnakeCase("getHttpResponseCode")).isEqualTo("get_http_response_code")
  }
}