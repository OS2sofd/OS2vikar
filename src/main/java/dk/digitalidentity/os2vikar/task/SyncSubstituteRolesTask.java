package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.SubstituteService;

@Component
@EnableScheduling
public class SyncSubstituteRolesTask {

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private OS2VikarConfiguration config;

	// TODO: most of the logic is tied to a workplace start/stop dates, so once per day is fine for those, the rest
	//       is tied to newly created workplaces... perhaps a flag to indicate if we NEED to run this task in full,
	//       would be a better way to save some DB processing (and avoid some of the deadlocks/timeouts we are seeing
	//       in the synchronize to SOFD

	// every minute
	@Scheduled(fixedDelay = 60 * 1000)
	public void execute() {
		if (config.isScheduledJobsEnabled()) {
			substituteService.sync();
		}
	}
}
