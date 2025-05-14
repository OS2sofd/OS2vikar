package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.LocalRoleDao;
import dk.digitalidentity.os2vikar.dao.model.LocalRole;

@Service
public class LocalRoleService {

	@Autowired
	private LocalRoleDao localRoleDao;
	
	public List<LocalRole> getAll() {
		return localRoleDao.findAll();
	}
	
	public LocalRole getById(long id) {
		return localRoleDao.findByid(id);
	}
	
	public LocalRole save(LocalRole localRole) {
		return localRoleDao.save(localRole);
	}
	
	public void delete(LocalRole localRole) {
		localRoleDao.delete(localRole);
	}
}
