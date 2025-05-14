package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.PasswordChangeQueue;
import dk.digitalidentity.os2vikar.dao.model.enums.ReplicationStatus;

public interface PasswordChangeQueueDao extends JpaRepository<PasswordChangeQueue, Long> {
	List<PasswordChangeQueue> findAll();
	List<PasswordChangeQueue> findByStatus(ReplicationStatus replicationStatus);
	List<PasswordChangeQueue> findByStatusNotIn(ReplicationStatus... replicationStatus);
	List<PasswordChangeQueue> findBySamaccountNameAndStatusNot(String samaccountName, ReplicationStatus replicationStatus);
	PasswordChangeQueue findFirst1ByStatusNotOrderByTtsAsc(ReplicationStatus replicationStatus);
}
