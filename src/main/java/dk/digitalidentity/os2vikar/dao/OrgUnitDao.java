package dk.digitalidentity.os2vikar.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.os2vikar.dao.model.OrgUnit;

public interface OrgUnitDao extends JpaRepository<OrgUnit, String>{
	OrgUnit findByUuid(String uuid);
	List<OrgUnit> findByCanHaveSubstitutesTrue();
    List<OrgUnit> findByUuidIn(List<String> uuidList);
}
