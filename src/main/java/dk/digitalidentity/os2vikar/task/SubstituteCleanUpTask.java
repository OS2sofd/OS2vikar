package dk.digitalidentity.os2vikar.task;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.SubstituteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SubstituteCleanUpTask {

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private OS2VikarConfiguration config;

	@Scheduled(cron = "${cron.substituteCleanup:0 #{new java.util.Random().nextInt(55)} 3 * * ?}")
	public void execute() {
		if (!config.isScheduledJobsEnabled()) {
			return;
		}
		
		substituteService.cleanUp();
	}
}
