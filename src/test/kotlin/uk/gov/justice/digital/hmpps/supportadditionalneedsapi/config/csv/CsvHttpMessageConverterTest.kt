package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.csv

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotWritableException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class CsvHttpMessageConverterTest {

  private lateinit var converter: CsvHttpMessageConverter
  private lateinit var outputMessage: HttpOutputMessage
  private lateinit var outputStream: ByteArrayOutputStream

  @BeforeEach
  fun setUp() {
    converter = CsvHttpMessageConverter()
    outputStream = ByteArrayOutputStream()
    outputMessage = object : HttpOutputMessage {
      override fun getBody(): OutputStream = outputStream
      override fun getHeaders(): HttpHeaders = HttpHeaders()
    }
  }

  @Test
  fun `canWrite should return true for Collection types with CSV media type`() {
    val csvMediaType = MediaType.parseMediaType("text/csv")
    assertThat(converter.canWrite(List::class.java, csvMediaType)).isTrue()
    assertThat(converter.canWrite(Set::class.java, csvMediaType)).isTrue()
    assertThat(converter.canWrite(Collection::class.java, csvMediaType)).isTrue()
  }

  @Test
  fun `canWrite should return true for CsvSerializable annotated classes`() {
    val csvMediaType = MediaType.parseMediaType("text/csv")
    assertThat(converter.canWrite(TestCsvRecord::class.java, csvMediaType)).isTrue()
  }

  @Test
  fun `canWrite should return false for non-annotated classes`() {
    val csvMediaType = MediaType.parseMediaType("text/csv")
    assertThat(converter.canWrite(NonCsvSerializable::class.java, csvMediaType)).isFalse()
  }

  @Test
  fun `canRead should always return false`() {
    assertThat(converter.canRead(List::class.java, MediaType.parseMediaType("text/csv"))).isFalse()
    assertThat(converter.canRead(TestCsvRecord::class.java, null)).isFalse()
  }

  @Test
  fun `write should output empty string for empty collection`() {
    val emptyList = emptyList<TestCsvRecord>()
    
    converter.write(emptyList, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8)
    assertThat(result).isEmpty()
  }

  @Test
  fun `write should output CSV with headers for collection`() {
    val records = listOf(
      TestCsvRecord("id1", "Alice", 25),
      TestCsvRecord("id2", "Bob", 30)
    )
    
    converter.write(records, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8).trim()
    val lines = result.lines()
    
    assertThat(lines).hasSize(3)
    // CSV mapper may order columns differently - check content exists
    assertThat(lines[0]).contains("id", "name", "age")
    assertThat(lines[1]).contains("id1", "Alice", "25")
    assertThat(lines[2]).contains("id2", "Bob", "30")
  }

  @Test
  fun `write should respect JsonPropertyOrder annotation`() {
    val records = listOf(
      TestCsvRecordWithOrder("id1", "Alice", 25)
    )
    
    converter.write(records, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8).trim()
    val lines = result.lines()
    
    assertThat(lines).hasSize(2)
    assertThat(lines[0]).isEqualTo("name,age,id")
    assertThat(lines[1]).isEqualTo("Alice,25,id1")
  }

  @Test
  fun `write should output CSV for single CsvSerializable object`() {
    val record = TestCsvRecord("id1", "Alice", 25)
    
    converter.write(record, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8).trim()
    val lines = result.lines()
    
    assertThat(lines).hasSize(2)
    // CSV mapper may order columns differently - check content exists
    assertThat(lines[0]).contains("id", "name", "age")
    assertThat(lines[1]).contains("id1", "Alice", "25")
  }

  @Test
  fun `write should throw exception for non-CsvSerializable single object`() {
    val nonSerializable = NonCsvSerializable("test")
    
    assertThatThrownBy {
      converter.write(nonSerializable, MediaType.parseMediaType("text/csv"), outputMessage)
    }.isInstanceOf(HttpMessageNotWritableException::class.java)
      .hasMessageContaining("NonCsvSerializable is not marked with @CsvSerializable annotation")
  }

  @Test
  fun `write should throw exception for collection with null elements`() {
    val listWithNull = listOf<TestCsvRecord?>(null)
    
    assertThatThrownBy {
      converter.write(listWithNull, MediaType.parseMediaType("text/csv"), outputMessage)
    }.isInstanceOf(HttpMessageNotWritableException::class.java)
      .hasMessage("Cannot generate CSV: collection contains null elements")
  }

  @Test
  fun `write should handle collection of non-CsvSerializable with warning`() {
    val records = listOf(
      NonCsvSerializable("test1"),
      NonCsvSerializable("test2")
    )
    
    // Should still write CSV, but with warning logged
    converter.write(records, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8)
    assertThat(result).isNotEmpty()
  }

  @Test
  fun `write should use JsonProperty annotations for column names`() {
    val records = listOf(
      TestCsvRecordSnakeCase("id1", "Alice Smith", 25)
    )
    
    converter.write(records, MediaType.parseMediaType("text/csv"), outputMessage)
    
    val result = outputStream.toString(StandardCharsets.UTF_8).trim()
    val lines = result.lines()
    
    assertThat(lines).hasSize(2)
    assertThat(lines[0]).contains("record_id", "full_name", "person_age")
    assertThat(lines[1]).contains("id1", "Alice Smith", "25")
  }

  @Test
  fun `write should handle IOException gracefully`() {
    // Create an OutputStream that throws IOException when writing
    val failingStream = object : OutputStream() {
      override fun write(b: Int) {
        throw IOException("Simulated IO Error")
      }
    }
    
    val failingOutputMessage = object : HttpOutputMessage {
      override fun getBody(): OutputStream = failingStream
      override fun getHeaders(): HttpHeaders = HttpHeaders()
    }
    
    val records = listOf(TestCsvRecord("id1", "Alice", 25))
    
    assertThatThrownBy {
      converter.write(records, MediaType.parseMediaType("text/csv"), failingOutputMessage)
    }.isInstanceOf(IOException::class.java)
      .hasMessage("Simulated IO Error")
  }

  // Test data classes
  @CsvSerializable
  data class TestCsvRecord(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("age")
    val age: Int
  )

  @CsvSerializable
  @JsonPropertyOrder("name", "age", "id")
  data class TestCsvRecordWithOrder(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("age")
    val age: Int
  )

  @CsvSerializable
  data class TestCsvRecordSnakeCase(
    @JsonProperty("record_id")
    val recordId: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("person_age")
    val personAge: Int
  )

  data class NonCsvSerializable(
    val value: String
  )
}