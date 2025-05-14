package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.RoleCatalogueService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class FetchUserRolesFromRoleCatalogTask {

	@Autowired
	private RoleCatalogueService roleCatalogService;
	
	@Autowired
	private OS2VikarConfiguration config;

	// a handfuld of times a day
    @Scheduled(cron = "${cron.rc.sync:0 #{new java.util.Random().nextInt(55)} 3,10,14,19 * * ?}")
	public synchronized void synchronizeUserRoles() {
		if (!config.isScheduledJobsEnabled() || !config.getRc().isEnabled()) {
			return;
		}

		log.info("Running scheduled job. RoleCatalog It-System/UserRole Synchronization");

		roleCatalogService.syncData();
	}
}
