package dk.digitalidentity.os2vikar.task;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class SyncADGroupsTask {

	@Autowired
	private SubstituteService substituteService;

	@Autowired
	private OS2VikarConfiguration config;

	@Scheduled(cron = "${cron.ad.fullsync:0 #{new java.util.Random().nextInt(55)} 2 * * ?}")
	public void execute() throws Exception {
		if (!config.isScheduledJobsEnabled() || !config.getSyncADGroups().isEnabled()) {
			return;
		}

		log.info("Starting AD group sync task");
		substituteService.syncAllADGroups();
		log.info("Finished AD group sync task");
	}
}
