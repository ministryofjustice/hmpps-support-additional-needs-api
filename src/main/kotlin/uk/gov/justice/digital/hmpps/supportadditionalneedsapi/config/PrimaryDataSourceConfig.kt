package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  entityManagerFactoryRef = "primaryEntityManagerFactory",
  transactionManagerRef = "primaryTransactionManager",
  basePackages = ["uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository"],
  excludeFilters = [
    org.springframework.context.annotation.ComponentScan.Filter(
      type = org.springframework.context.annotation.FilterType.REGEX,
      pattern = ["uk\\.gov\\.justice\\.digital\\.hmpps\\.supportadditionalneedsapi\\.domain\\.repository\\.reporting\\..*"],
    ),
  ],
)
class PrimaryDataSourceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource-primary")
  fun primaryDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean(name = ["primaryDataSource"])
  @Primary
  @ConfigurationProperties("spring.datasource-primary.hikari")
  fun primaryDataSource(
    @Qualifier("primaryDataSourceProperties") dataSourceProperties: DataSourceProperties,
  ): DataSource = dataSourceProperties
    .initializeDataSourceBuilder()
    .build()

  @Bean
  @Primary
  @ConfigurationProperties("spring.jpa")
  fun primaryJpaProperties(): JpaProperties = JpaProperties()

  @Bean(name = ["primaryEntityManagerFactory"])
  @Primary
  fun primaryEntityManagerFactory(
    @Qualifier("primaryDataSource") dataSource: DataSource,
    @Qualifier("primaryJpaProperties") jpaProperties: JpaProperties,
  ): LocalContainerEntityManagerFactoryBean = LocalContainerEntityManagerFactoryBean().apply {
    setDataSource(dataSource)
    setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
    persistenceUnitName = "primary"
    setPackagesToScan("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity")

    val properties = HashMap<String, Any?>()
    properties.putAll(jpaProperties.properties)
    properties["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQLDialect"
    setJpaPropertyMap(properties)
  }

  @Bean(name = ["primaryTransactionManager"])
  @Primary
  fun primaryTransactionManager(
    @Qualifier("primaryEntityManagerFactory") entityManagerFactory: LocalContainerEntityManagerFactoryBean,
  ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory.getObject()!!)
}
