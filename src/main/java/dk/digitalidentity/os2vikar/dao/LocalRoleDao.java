package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.LocalRole;

public interface LocalRoleDao extends JpaRepository<LocalRole, Long>{
	LocalRole findByid(long id);
}
