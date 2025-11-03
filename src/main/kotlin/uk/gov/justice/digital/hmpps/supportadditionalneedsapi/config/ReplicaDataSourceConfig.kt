package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  entityManagerFactoryRef = "replicaEntityManagerFactory",
  transactionManagerRef = "replicaTransactionManager",
  basePackages = ["uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.reporting"],
)
class ReplicaDataSourceConfig {

  @Bean
  @ConfigurationProperties("spring.datasource-replica")
  fun replicaDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean(name = ["replicaDataSource"])
  @ConfigurationProperties("spring.datasource-replica.hikari")
  fun replicaDataSource(
    @Qualifier("replicaDataSourceProperties") dataSourceProperties: DataSourceProperties,
  ): DataSource = dataSourceProperties
    .initializeDataSourceBuilder()
    .build()

  @Bean(name = ["replicaEntityManagerFactory"])
  fun replicaEntityManagerFactory(
    @Qualifier("replicaDataSource") dataSource: DataSource,
    @Qualifier("primaryJpaProperties") jpaProperties: JpaProperties,
  ): LocalContainerEntityManagerFactoryBean = LocalContainerEntityManagerFactoryBean().apply {
    setDataSource(dataSource)
    setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
    persistenceUnitName = "replica"
    setPackagesToScan("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity")

    val properties = HashMap<String, Any?>()
    properties.putAll(jpaProperties.properties)
    properties["hibernate.physical_naming_strategy"] = "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
    properties["hibernate.connection.readOnly"] = true
    setJpaPropertyMap(properties)
  }

  @Bean(name = ["replicaTransactionManager"])
  fun replicaTransactionManager(
    @Qualifier("replicaEntityManagerFactory") entityManagerFactory: LocalContainerEntityManagerFactoryBean,
  ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory.getObject()!!).apply {
    setJpaPropertyMap(mapOf("hibernate.connection.readOnly" to true))
  }
}
