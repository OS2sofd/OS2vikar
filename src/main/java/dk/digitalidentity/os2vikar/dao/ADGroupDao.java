package dk.digitalidentity.os2vikar.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.ADGroup;

public interface ADGroupDao extends JpaRepository<ADGroup, Long> {
	ADGroup findByObjectGuid(String objectGuid);
}
