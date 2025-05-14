package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.GlobalTitle;

public interface GlobalTitleDao extends JpaRepository<GlobalTitle, Long>{
	GlobalTitle findByid(long id);
}
