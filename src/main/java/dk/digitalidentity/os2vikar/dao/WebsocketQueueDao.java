package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.WebsocketQueue;

public interface WebsocketQueueDao extends JpaRepository<WebsocketQueue, Long> {
	List<WebsocketQueue> findAll();
}
