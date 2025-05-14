package dk.digitalidentity.os2vikar.task;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class AuditLogCleanUpTask {

	@Autowired
	private AuditLogService auditLogService;

	@Autowired
	private OS2VikarConfiguration config;

	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 1 * * ?")
	public void execute() {
		if (!config.isScheduledJobsEnabled()) {
			return;
		}

		auditLogService.cleanUp();
	}
}
