package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.AuditLog;

public interface AuditLogDao extends JpaRepository<AuditLog, Long> {
	AuditLog findById(long id);

	long deleteByTimestampBefore(LocalDateTime timestamp);
}
