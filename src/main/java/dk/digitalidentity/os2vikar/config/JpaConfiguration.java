package dk.digitalidentity.os2vikar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EnableJpaRepositories(basePackages = { "dk.digitalidentity.os2vikar.dao" }, repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
public class JpaConfiguration {

}