package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.os2vikar.dao.model.UserRole;

public interface UserRoleDao extends CrudRepository<UserRole, Long> {
	List<UserRole> findAll();

	UserRole getById(long id);
}
