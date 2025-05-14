package dk.digitalidentity.os2vikar.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.dao.model.PasswordChangeQueue;
import dk.digitalidentity.os2vikar.dao.model.enums.ReplicationStatus;
import dk.digitalidentity.os2vikar.service.model.ADResponse;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Slf4j
@Service
public class ADPasswordService {

	@Autowired
	private PasswordChangeQueueService passwordChangeQueueService;

	@Autowired
	private WebSocketService websocketService;

	@Transactional(rollbackFor = Exception.class)
	public void syncPasswordsToAD() {

		for (PasswordChangeQueue change : passwordChangeQueueService.getUnsynchronized()) {
			attemptPasswordReplication(change);
			
			passwordChangeQueueService.save(change);
		}
	}

	public ADResponse.ADStatus attemptPasswordReplication(PasswordChangeQueue change) {

		try {
			ADResponse result = websocketService.setPassword(change.getSamaccountName(), change.getPassword());

			if (result == null) {
				log.error("No result on response");
				return ADStatus.TECHNICAL_ERROR;
			}

			if (ADStatus.OK.equals(result.getStatus())) {
				change.setStatus(ReplicationStatus.SYNCHRONIZED);
				change.setMessage(null);
			}
			else {
				// Setting status and message of change
				change.setStatus(ReplicationStatus.ERROR);
				String changeMessage = " Message: " + ((result.getMessage() != null) ? result.getMessage() : "NULL");
				change.setMessage(changeMessage);

				// Logging error/warn depending on how long it has gone unsynchronized
				if (change.getTts() != null && LocalDateTime.now().minusMinutes(10).isAfter(change.getTts())) {
					log.error("Replication failed, password change has not been replicated for more than 10 minutes (ID: " + change.getId() + ")");
					change.setStatus(ReplicationStatus.FINAL_ERROR);
				}
				else {
					log.warn("Password Replication failed, trying again in 1 minute (ID: " + change.getId() + ")");
				}
			}

			return result.getStatus();
		}
		catch (Exception ex) {
			change.setStatus(ReplicationStatus.ERROR);
			change.setMessage("Failed to connect to AD Password replication service: " + ex.getMessage());

			// tts null check to avoid issues with first attempt
			if (change.getTts() != null && LocalDateTime.now().minusMinutes(10).isAfter(change.getTts())) {
				log.error("Replication failed, password change has not been replicated for more than 10 minutes (ID: " + change.getId() + ")", ex);
				change.setStatus(ReplicationStatus.FINAL_ERROR);
			}
			else {
				log.warn("Password Replication failed, trying again in 1 minute (ID: " + change.getId() + ")");
			}
		}

		return ADStatus.TECHNICAL_ERROR;
	}

	@Transactional(rollbackFor = Exception.class)
	public void syncQueueCleanupTask() {

		// delete successful after 7 days
		List<PasswordChangeQueue> synchronizedChanges = passwordChangeQueueService.getByStatus(ReplicationStatus.SYNCHRONIZED);

		for (PasswordChangeQueue synchronizedChange : synchronizedChanges) {
			LocalDateTime maxRetention = LocalDateTime.now().minusDays(7);

			if (synchronizedChange.getTts().isBefore(maxRetention)) {
				passwordChangeQueueService.delete(synchronizedChange);
			}
		}

		// then delete failures after 21 days
		synchronizedChanges = passwordChangeQueueService.getByStatus(ReplicationStatus.FINAL_ERROR);

		for (PasswordChangeQueue synchronizedChange : synchronizedChanges) {
			LocalDateTime maxRetention = LocalDateTime.now().minusDays(21);

			if (synchronizedChange.getTts().isBefore(maxRetention)) {
				passwordChangeQueueService.delete(synchronizedChange);
			}
		}
	}
}
