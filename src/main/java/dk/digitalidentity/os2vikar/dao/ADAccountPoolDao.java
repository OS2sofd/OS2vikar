package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.ADAccountPool;
import dk.digitalidentity.os2vikar.dao.model.enums.ADAccountPoolStatus;

public interface ADAccountPoolDao extends JpaRepository<ADAccountPool, Long>{
	List<ADAccountPool> findByStatusAndWithO365License(ADAccountPoolStatus status, boolean withO365License);
}
