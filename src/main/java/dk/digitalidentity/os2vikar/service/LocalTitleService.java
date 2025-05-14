package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.LocalTitleDao;
import dk.digitalidentity.os2vikar.dao.model.LocalTitle;

@Service
public class LocalTitleService {

	@Autowired
	private LocalTitleDao localTitleDao;
	
	public List<LocalTitle> getAll() {
		return localTitleDao.findAll();
	}
	
	public LocalTitle getById(long id) {
		return localTitleDao.findByid(id);
	}
	
	public LocalTitle save(LocalTitle localTitle) {
		return localTitleDao.save(localTitle);
	}
	
	public void delete(LocalTitle localTitle) {
		localTitleDao.delete(localTitle);
	}
}
