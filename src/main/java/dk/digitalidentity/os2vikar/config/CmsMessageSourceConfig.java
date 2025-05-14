package dk.digitalidentity.os2vikar.config;

import dk.digitalidentity.os2vikar.service.CmsMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CmsMessageSourceConfig {

	@Bean
	public CmsMessageSource cmsMessageSource() {
		CmsMessageSource messageSource = new CmsMessageSource();
		messageSource.setBasename("classpath:cms-messages");
		messageSource.setDefaultEncoding("UTF-8");

		return messageSource;
	}
}
