package dk.digitalidentity.os2vikar.service;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.PasswordChangeQueueDao;
import dk.digitalidentity.os2vikar.dao.model.PasswordChangeQueue;
import dk.digitalidentity.os2vikar.dao.model.enums.ReplicationStatus;
import dk.digitalidentity.os2vikar.service.model.ADResponse.ADStatus;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PasswordChangeQueueService {
	
	@Autowired
	private ADPasswordService adPasswordService;

	@Autowired
	private PasswordChangeQueueDao passwordChangeQueueDao;

	public PasswordChangeQueue save(PasswordChangeQueue passwordChangeQueue) {
		// if the user tries to change password multiple times in a row, we only want to keep the latest - this
		// removes any attempts in the queue that is not already synchronized (which we need to keep for debugging purposes)

		if (passwordChangeQueue.getId() == 0) {
			List<PasswordChangeQueue> oldQueued = passwordChangeQueueDao.findBySamaccountNameAndStatusNot(passwordChangeQueue.getSamaccountName(), ReplicationStatus.SYNCHRONIZED);

			if (oldQueued != null && oldQueued.size() > 0) {
				passwordChangeQueueDao.deleteAll(oldQueued);
			}
		}

		return passwordChangeQueueDao.save(passwordChangeQueue);
	}

	public void delete(PasswordChangeQueue passwordChangeQueue) {
		passwordChangeQueueDao.delete(passwordChangeQueue);
	}

	public List<PasswordChangeQueue> getAll() {
		return passwordChangeQueueDao.findAll();
	}

	public List<PasswordChangeQueue> getByStatus(ReplicationStatus status) {
		return passwordChangeQueueDao.findByStatus(status);
	}

	public List<PasswordChangeQueue> getUnsynchronized() {
		return passwordChangeQueueDao.findByStatusNotIn(ReplicationStatus.SYNCHRONIZED, ReplicationStatus.FINAL_ERROR);
	}

	public ADStatus attemptPasswordChange(String samaccountName, String newPassword) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
		PasswordChangeQueue change = new PasswordChangeQueue(samaccountName, newPassword);

		ADStatus status = adPasswordService.attemptPasswordReplication(change);
		switch (status) {
			// inform user through UI
			case FAILURE:
			case TECHNICAL_ERROR:
				break;

			case NOOP:
				log.error("Got a NOOP case here - that should not happen");
				break;

			// save result - so it is correctly logged to the queue
			case OK:
				save(change);
				break;

			// delay replication in case of a timeout
			case TIMEOUT:
			case NO_CONNECTION:
				save(change);
				break;
		}

		return status;
	}

	public PasswordChangeQueue getOldestUnsynchronizedByDomain() {
		return passwordChangeQueueDao.findFirst1ByStatusNotOrderByTtsAsc(ReplicationStatus.SYNCHRONIZED);
	}
}
