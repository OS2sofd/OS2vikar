package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.ADPasswordService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SyncPasswordToADTask {

	@Autowired
	private OS2VikarConfiguration configuration;

	@Autowired
	private ADPasswordService adPasswordService;

	// every minute
	@Scheduled(fixedDelay = 60 * 1000)
	public void processChanges() {
		if (configuration.isScheduledJobsEnabled()) {
			log.debug("Sync password to AD via Websockets");

			adPasswordService.syncPasswordsToAD();
		}
	}
}
