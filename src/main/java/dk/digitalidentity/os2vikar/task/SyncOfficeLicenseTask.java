package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.SubstituteService;

@Component
@EnableScheduling
public class SyncOfficeLicenseTask {

	@Autowired
	private OS2VikarConfiguration configuration;

	@Autowired
	private SubstituteService substituteService;

	// nightly ensure all licenses are correctly set
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 1 * * ?")
	public void processChanges() {
		if (configuration.isScheduledJobsEnabled()) {
			substituteService.syncOfficeLicenseTask();
		}
	}
}
