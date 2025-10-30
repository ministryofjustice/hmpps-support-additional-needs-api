package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.csv

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.util.StringUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

class CsvHttpMessageConverter : AbstractHttpMessageConverter<Any>(
  MediaType("text", "csv", StandardCharsets.UTF_8),
  MediaType("application", "csv", StandardCharsets.UTF_8)
) {
  companion object {
    private val log = KotlinLogging.logger {}
  }

  private val csvMapper = CsvMapper().apply {
    registerModule(KotlinModule.Builder().build())
  }

  override fun supports(clazz: Class<*>): Boolean {
    val isSupported = Collection::class.java.isAssignableFrom(clazz) || 
           clazz.kotlin.hasAnnotation<CsvSerializable>()
    log.debug { "Checking CSV support for ${clazz.simpleName}: $isSupported" }
    return isSupported
  }

  override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean {
    return false // We don't support reading CSV for now
  }

  @Throws(IOException::class, HttpMessageNotReadableException::class)
  override fun readInternal(clazz: Class<out Any>, inputMessage: HttpInputMessage): Any {
    throw UnsupportedOperationException("CSV deserialization is not supported")
  }

  @Throws(IOException::class, HttpMessageNotWritableException::class)
  override fun writeInternal(t: Any, outputMessage: HttpOutputMessage) {
    outputMessage.body.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
      try {
        when (t) {
          is Collection<*> -> writeCollection(t, writer)
          else -> writeSingleObject(t, writer)
        }
      } catch (e: Exception) {
        handleWriteException(e, t::class.simpleName ?: "Unknown")
      }
    }
  }
  
  private fun writeCollection(collection: Collection<*>, writer: java.io.Writer) {
    log.debug { "Converting collection of ${collection.size} items to CSV" }
    
    if (collection.isEmpty()) {
      log.debug { "Writing empty response for empty collection" }
      writer.write("")
      return
    }
    
    val firstItem = collection.firstOrNull()
      ?: throw HttpMessageNotWritableException("Cannot generate CSV: collection contains null elements").also {
        log.warn { "Collection contains null first element, cannot determine schema" }
      }
    
    val elementType = firstItem::class
    writeCsvData(elementType, collection, writer, "collection of ${collection.size}")
  }
  
  private fun writeSingleObject(obj: Any, writer: java.io.Writer) {
    val objType = obj::class
    
    if (!objType.hasAnnotation<CsvSerializable>()) {
      val errorMessage = "Object of type ${objType.simpleName} is not marked with @CsvSerializable annotation"
      log.error { errorMessage }
      throw HttpMessageNotWritableException(errorMessage)
    }
    
    writeCsvData(objType, listOf(obj), writer, "single")
  }
  
  private fun writeCsvData(
    dataType: KClass<*>,
    data: Collection<*>,
    writer: java.io.Writer,
    dataDescription: String
  ) {
    log.debug { "Processing $dataDescription ${dataType.simpleName} object(s)" }
    
    if (!dataType.hasAnnotation<CsvSerializable>()) {
      log.warn { "Type ${dataType.simpleName} is not marked with @CsvSerializable" }
    }
    
    val csvWriter = createCsvWriter(dataType)
    writer.write(csvWriter.writeValueAsString(data))
    log.debug { "Successfully wrote $dataDescription item(s) to CSV" }
  }
  
  private fun createCsvWriter(clazz: KClass<*>): ObjectWriter {
    val schema = buildCsvSchema(clazz)
    return csvMapper.writer(schema.withHeader())
  }
  
  private fun handleWriteException(e: Exception, typeName: String) {
    when (e) {
      is HttpMessageNotWritableException -> throw e
      is IOException -> {
        val errorMessage = "Failed to generate CSV for $typeName: ${e.message}"
        log.error(e) { errorMessage }
        throw HttpMessageNotWritableException(errorMessage, e)
      }
      else -> {
        val errorMessage = "Unexpected error generating CSV for $typeName: ${e.message}"
        log.error(e) { errorMessage }
        throw HttpMessageNotWritableException(errorMessage, e)
      }
    }
  }

  private fun buildCsvSchema(clazz: KClass<*>): CsvSchema {
    log.debug { "Building CSV schema for ${clazz.simpleName}" }
    val builder = CsvSchema.builder()
    
    // Verify class has CsvSerializable annotation for better design
    if (!clazz.hasAnnotation<CsvSerializable>()) {
      log.debug { "Class ${clazz.simpleName} does not have @CsvSerializable annotation, proceeding with reflection" }
    }
    
    // Check if class has JsonPropertyOrder annotation
    val propertyOrder = clazz.findAnnotation<JsonPropertyOrder>()
    
    if (propertyOrder != null && propertyOrder.value.isNotEmpty()) {
      log.debug { "Using JsonPropertyOrder for ${clazz.simpleName}: ${propertyOrder.value.joinToString()}" }
      propertyOrder.value.forEach { fieldName ->
        builder.addColumn(fieldName)
      }
    } else {
      // Use reflection to get all properties
      val properties = clazz.memberProperties.toList()
      log.debug { "Using reflection for ${clazz.simpleName}, found ${properties.size} properties" }
      
      properties.forEach { property ->
        val jsonProperty = property.findAnnotation<JsonProperty>()
        val columnName = jsonProperty?.value ?: StringUtils.toSnakeCase(property.name)
        builder.addColumn(columnName)
        log.trace { "Added column: $columnName for property: ${property.name}" }
      }
    }
    
    val schema = builder.build()
    log.debug { "Built CSV schema with ${schema.size()} columns for ${clazz.simpleName}" }
    return schema
  }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CsvSerializable