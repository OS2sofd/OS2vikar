package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.GlobalTitleDao;
import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;

@Service
public class GlobalTitleService {

	@Autowired
	private GlobalTitleDao globalTitleDao;
	
	public List<GlobalTitle> getAll() {
		return globalTitleDao.findAll();
	}
	
	public GlobalTitle getById(long id) {
		return globalTitleDao.findByid(id);
	}
	
	public GlobalTitle save(GlobalTitle globalTitle) {
		return globalTitleDao.save(globalTitle);
	}
	
	public void delete(GlobalTitle globalTitle) {
		globalTitleDao.delete(globalTitle);
	}
}
