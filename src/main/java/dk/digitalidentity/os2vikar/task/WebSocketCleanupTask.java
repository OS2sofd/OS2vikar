package dk.digitalidentity.os2vikar.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.service.SocketHandler;

@EnableScheduling
@Component
public class WebSocketCleanupTask {

	@Autowired
	private SocketHandler socketHandler;
	
	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void cleanup() {
		socketHandler.cleanupRequestResponse();
		socketHandler.closeStaleSessions();
	}
}
