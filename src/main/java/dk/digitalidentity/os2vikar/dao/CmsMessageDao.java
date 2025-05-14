package dk.digitalidentity.os2vikar.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.CmsMessage;

public interface CmsMessageDao extends JpaRepository<CmsMessage, Long> {
	List<CmsMessage> findAll();
	List<CmsMessage> findAllByLastUpdatedAfter(LocalDateTime after);
	CmsMessage findByCmsKey(String cmsKey);
}
