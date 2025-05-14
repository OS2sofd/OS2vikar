package dk.digitalidentity.os2vikar.task;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.ADAccountPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class CreateADAccountTask {

	@Autowired
	private ADAccountPoolService adAccountPoolService;

	@Autowired
	private OS2VikarConfiguration config;

	// every 10 minutes
	@Scheduled(cron = "${vikar.ad.cron:0 0/10 * * * ?}")
	public void execute() {
		if (!config.isScheduledJobsEnabled()) {
			return;
		}

		adAccountPoolService.create();
	}
}
