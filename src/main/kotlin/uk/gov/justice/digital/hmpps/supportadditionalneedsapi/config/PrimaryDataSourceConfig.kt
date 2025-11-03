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
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  entityManagerFactoryRef = "primaryEntityManagerFactory",
  transactionManagerRef = "primaryTransactionManager",
  basePackages = ["uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository"],
)
class PrimaryDataSourceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource-primary")
  fun primaryDataSourceProperties() = DataSourceProperties()

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource-primary.hikari")
  fun primaryDataSource(primaryDataSourceProperties: DataSourceProperties): DataSource = primaryDataSourceProperties.initializeDataSourceBuilder().build()

  @Bean
  @Primary
  @DependsOn("flyway")
  fun primaryEntityManagerFactory(
    @Qualifier("primaryDataSource") dataSource: DataSource,
    builder: EntityManagerFactoryBuilder,
    jpaProperties: JpaProperties,
    hibernateProperties: HibernateProperties,
  ): LocalContainerEntityManagerFactoryBean {
    val properties = hibernateProperties.determineHibernateProperties(
      jpaProperties.properties,
      HibernateSettings(),
    )
    return builder
      .dataSource(dataSource)
      .packages("uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity")
      .persistenceUnit("primary")
      .properties(properties)
      .build()
  }

  @Bean
  @Primary
  fun primaryTransactionManager(@Qualifier("primaryEntityManagerFactory") primaryEntityManagerFactory: LocalContainerEntityManagerFactoryBean) = JpaTransactionManager(primaryEntityManagerFactory.`object`!!)
}
