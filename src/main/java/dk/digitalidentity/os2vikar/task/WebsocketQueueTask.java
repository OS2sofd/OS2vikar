package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.service.WebsocketQueueService;

@Component
@EnableScheduling
public class WebsocketQueueTask {

	@Autowired
	private OS2VikarConfiguration configuration;

	@Autowired
	private WebsocketQueueService websocketQueueService;

	@Scheduled(fixedDelay = 60 * 1000)
	public void processQueue() {
		if (configuration.isScheduledJobsEnabled()) {
			websocketQueueService.sync();
		}
	}
}
