package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.GlobalRoleDao;
import dk.digitalidentity.os2vikar.dao.model.GlobalRole;

@Service
public class GlobalRoleService {

	@Autowired
	private GlobalRoleDao globalRoleDao;
	
	public List<GlobalRole> getAll() {
		return globalRoleDao.findAll();
	}
	
	public GlobalRole getById(long id) {
		return globalRoleDao.findByid(id);
	}
	
	public GlobalRole save(GlobalRole globalRole) {
		return globalRoleDao.save(globalRole);
	}
	
	public void delete(GlobalRole globalRole) {
		globalRoleDao.delete(globalRole);
	}
}
