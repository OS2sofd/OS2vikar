package dk.digitalidentity.os2vikar.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.config.Commands;
import dk.digitalidentity.os2vikar.dao.WebsocketQueueDao;
import dk.digitalidentity.os2vikar.dao.model.WebsocketQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebsocketQueueService {

	@Autowired
	private WebsocketQueueDao websocketQueueDao;
	
	@Autowired
	private WebSocketService websockeService;

	public WebsocketQueue save(WebsocketQueue queue) {
		return websocketQueueDao.save(queue);
	}

	@Transactional
	public void sync() {
		LocalDateTime now = LocalDateTime.now();
		
		for (WebsocketQueue queue : websocketQueueDao.findAll()) {
			if (now.isAfter(queue.getTts())) {
				log.info("Retrying websocket command: " + queue.getCommand() + " / " + queue.getTarget() + " / " + queue.getPayload());

				try {
					switch (queue.getCommand()) {
						case Commands.DELETE_ACCOUNT:
							websockeService.getSocketHandler().deleteAccountWithoutRetry(queue.getTarget());
							break;
						case Commands.DISABLE_ACCOUNT:
							websockeService.getSocketHandler().disableAccountWithoutRetry(queue.getTarget());
							break;
						case Commands.EMPLOYEE_SIGNATURE:
							websockeService.getSocketHandler().addToEmployeeSignatureGroupWithoutRetry(queue.getTarget());
							break;
						case Commands.AD_GROUPS_SYNC:
							websockeService.getSocketHandler().handleGroupMembershipsWithoutRetry(queue.getTarget(), queue.getPayload());
							break;
						case Commands.SET_EXPIRE:
							websockeService.getSocketHandler().setExpireWithoutRetry(queue.getTarget(), queue.getPayload());
							break;
						case Commands.UPDATE_LICENSE:
							websockeService.getSocketHandler().updateLicenseWithoutRetry(queue.getTarget(), "true".equals(queue.getPayload()));
							break;
						case Commands.UNLOCK_ACCOUNT:
							websockeService.getSocketHandler().unlockAccountWithoutRetry(queue.getTarget());
							break;
						default:
							log.error("Unknown command: " + queue.getCommand());
							break;
					}
				}
				catch (Exception ex) {
					log.error("Failed to perform retry of websocket command: " + queue.getCommand() + " / " + queue.getTarget() + " / " + queue.getPayload(), ex);
				}
				
				// no matter what, we remove it from the queue when retrying
				websocketQueueDao.delete(queue);
			}
		}
	}
}
