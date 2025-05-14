package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.GlobalRole;

public interface GlobalRoleDao extends JpaRepository<GlobalRole, Long>{
	GlobalRole findByid(long id);
}
