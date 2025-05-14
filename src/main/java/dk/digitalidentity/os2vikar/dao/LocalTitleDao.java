package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.LocalTitle;

public interface LocalTitleDao extends JpaRepository<LocalTitle, Long>{
	LocalTitle findByid(long id);
}
