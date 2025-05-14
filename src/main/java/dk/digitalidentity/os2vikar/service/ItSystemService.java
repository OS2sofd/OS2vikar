package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.ItSystemDao;
import dk.digitalidentity.os2vikar.dao.model.ItSystem;

@Service
public class ItSystemService {
	
	@Autowired
	private ItSystemDao itSystemDao;

	public List<ItSystem> findAll() {
		return itSystemDao.findAll();
	}

	public ItSystem getById(Long id) {
		return itSystemDao.getById(id);
	}

	public ItSystem save(ItSystem entity) {
		return itSystemDao.save(entity);
	}

	public void delete(ItSystem entity) {
		itSystemDao.delete(entity);
	}

	public void deleteAll(List<ItSystem> entities) {
		itSystemDao.deleteAll(entities);
	}
}
