package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.AuthorizationCodeService;

@Component
@EnableScheduling
public class AuthorizationCodesTask {

	@Autowired
	private OS2VikarConfiguration config;

	@Autowired
	private AuthorizationCodeService authorizationCodeService;

	// every 5 minutes
	@Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 3 * 60 * 1000)
	public void execute() {
		if (!config.isScheduledJobsEnabled() || !config.isEnableAuthorizationCodes()) {
			return;
		}

		authorizationCodeService.assignAuthorizationCodes();
	}
}
