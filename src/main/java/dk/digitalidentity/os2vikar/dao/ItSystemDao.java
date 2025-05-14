package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.os2vikar.dao.model.ItSystem;

public interface ItSystemDao extends CrudRepository<ItSystem, Long> {
	List<ItSystem> findAll();
	ItSystem getById(long id);
}
