package dk.digitalidentity.os2vikar.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.os2vikar.security.ApiSecurityFilter;

@Configuration
public class ApiSecurityFilterConfiguration {

	@Autowired
	private OS2VikarConfiguration config;

	@Bean
	public FilterRegistrationBean<ApiSecurityFilter> apiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter(config);

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/*");

		return filterRegistrationBean;
	}
}
