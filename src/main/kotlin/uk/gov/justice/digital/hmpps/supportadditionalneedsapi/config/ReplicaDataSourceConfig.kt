package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
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
  ): LocalContainerEntityManagerFactoryBean = LocalContainerEntityManagerFactoryBean().apply {
    setDataSource(dataSource)
    setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
    persistenceUnitName = "replica"
    setPackagesToScan("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity")

    val properties = HashMap<String, Any?>().apply {
      put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
      put("hibernate.hbm2ddl.auto", "validate")
      put("hibernate.show_sql", false)
      put("hibernate.format_sql", false)
      put("hibernate.connection.provider_disables_autocommit", true)
      put("hibernate.cache.use_second_level_cache", false)
      put("hibernate.cache.use_query_cache", false)
      put("org.hibernate.envers.audit_table_suffix", "_history")
      put("org.hibernate.envers.revision_field_name", "rev_id")
      put("org.hibernate.envers.revision_type_field_name", "rev_type")
      put("org.hibernate.envers.modified_flag_suffix", "_modified")
      put("org.hibernate.envers.store_data_at_delete", true)
      put("org.hibernate.envers.do_not_audit_optimistic_locking_field", false)
    }
    setJpaPropertyMap(properties)
  }

  @Bean(name = ["replicaTransactionManager"])
  fun replicaTransactionManager(
    @Qualifier("replicaEntityManagerFactory") entityManagerFactory: LocalContainerEntityManagerFactoryBean,
  ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory.getObject()!!)
}
