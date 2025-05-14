package dk.digitalidentity.os2vikar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.os2vikar.dao.UserRoleDao;
import dk.digitalidentity.os2vikar.dao.model.UserRole;

@Service
public class UserRoleService {

	@Autowired
	private UserRoleDao userRoleDao;

	public List<UserRole> findAll() {
		return userRoleDao.findAll();
	}

	public UserRole getById(Long id) {
		return userRoleDao.getById(id);
	}

	public UserRole save(UserRole entity) {
		return userRoleDao.save(entity);
	}

	public void delete(UserRole entity) {
		userRoleDao.delete(entity);
	}
}
