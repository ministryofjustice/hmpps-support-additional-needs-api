package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
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
  fun replicaDataSourceProperties() = DataSourceProperties()

  @Bean
  @ConfigurationProperties("spring.datasource-replica.hikari")
  fun replicaDataSource(replicaDataSourceProperties: DataSourceProperties): DataSource = replicaDataSourceProperties.initializeDataSourceBuilder().build()

  @Bean
  fun replicaEntityManagerFactory(
    @Qualifier("replicaDataSource") dataSource: DataSource,
    builder: EntityManagerFactoryBuilder,
    jpaProperties: JpaProperties,
    hibernateProperties: HibernateProperties,
  ): LocalContainerEntityManagerFactoryBean {
    val properties = hibernateProperties.determineHibernateProperties(
      jpaProperties.properties,
      HibernateSettings(),
    )
    properties["hibernate.hbm2ddl.auto"] = "none"
    return builder
      .dataSource(dataSource)
      .packages("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity")
      .persistenceUnit("replica")
      .properties(properties)
      .build()
  }

  @Bean
  fun replicaTransactionManager(@Qualifier("replicaEntityManagerFactory") replicaEntityManagerFactory: LocalContainerEntityManagerFactoryBean) = JpaTransactionManager(replicaEntityManagerFactory.`object`!!)
}
