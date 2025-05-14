package dk.digitalidentity.os2vikar.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.os2vikar.config.OS2VikarConfiguration;
import dk.digitalidentity.os2vikar.dao.OrgUnitDao;
import dk.digitalidentity.os2vikar.dao.model.OrgUnit;
import dk.digitalidentity.os2vikar.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrgUnitService {

	@Autowired
	private OrgUnitDao orgUnitDao;
	
	@Autowired
	private OS2VikarConfiguration config;
	
	public List<OrgUnit> getAll() {
		return orgUnitDao.findAll();
	}
	
	public void deleteAll(List<OrgUnit> orgUnits) {
		orgUnitDao.deleteAll(orgUnits);
	}
	
	public OrgUnit getByUuid(String uuid) {
		return orgUnitDao.findByUuid(uuid);
	}
	
	public OrgUnit save(OrgUnit orgUnit) {
		return orgUnitDao.save(orgUnit);
	}

	public List<OrgUnit> getWithSubstitutesAllowed() {
		return orgUnitDao.findByCanHaveSubstitutesTrue();
	}
	
	public List<String> getAllUuidsWithChildren(List<String> list) {
		return getAllWithChildren(list)
				.stream()
				.map(o -> o.getUuid())
				.collect(Collectors.toList());
	}
	
	public List<OrgUnit> getAllWithChildren(List<String> list) {
		List<OrgUnit> ous = orgUnitDao.findByUuidIn(list);
		Set<OrgUnit> result = new HashSet<>();
		
		for (OrgUnit ou : ous) {
			result.add(ou);

			for (OrgUnit child : ou.getChildren()) {
				getAllWithChildrenRecursive(result, child);
			}
		}
		
		return new ArrayList<>(result);
	}

	private void getAllWithChildrenRecursive(Set<OrgUnit> result, OrgUnit ou) {
		result.add(ou);
		for (OrgUnit child : ou.getChildren()) {
			getAllWithChildrenRecursive(result, child);
		}
	}

    public List<OrgUnit> getByUuids(List<String> uuidList) {
		return orgUnitDao.findByUuidIn(uuidList);
    }
    
    public List<OrgUnit> getAllOrgUnitsUserIsAllowedToCreateSubstitutesOn() {
		List<OrgUnit> allWithSubstitutesAllowed = getWithSubstitutesAllowed();

		// find potential constraint 1 - user is only allowed to manage substitutes on specific OUs
		Set<String> userConstraintOUUuids = new HashSet<>();
		userConstraintOUUuids.addAll(
			getAllUuidsWithChildren(
				new ArrayList<>(
					SecurityUtil.getOrgUnitUuidsFromConstraint(config.getConstraintOrgUnitIdentifier())
				)
			)
		);

		// find potential constraint 2 - user is only allowed to manage substitutes for specific it-systems
		Set<String> userConstrainedItSystems = SecurityUtil.getITSystemConstraint(config.getConstraintItSystemIdentifier());

		// filter orgunits based on both filters
		for (Iterator<OrgUnit> iterator = allWithSubstitutesAllowed.iterator(); iterator.hasNext();) {
			OrgUnit orgUnit = iterator.next();
			
			// if constrained on orgunit, and no match, then remove and skip to next
			if (!userConstraintOUUuids.isEmpty() && !userConstraintOUUuids.contains(orgUnit.getUuid())) {
				iterator.remove();
				continue;
			}

			// if constrained on it-system, and the orgunit is marked with it-systems, and no match, then remove and skip to next
			if (!userConstrainedItSystems.isEmpty()) {
				if (orgUnit.getItSystems() != null && !orgUnit.getItSystems().isEmpty()) {
					boolean match = false;
					
					for (String userConstrainedItSystem : userConstrainedItSystems) {
						for (String orgUnitConstrainedItSystem : orgUnit.getItSystems()) {
							if (Objects.equals(orgUnitConstrainedItSystem, userConstrainedItSystem)) {
								match = true;
								break;
							}
						}
						
						if (match ) {
							break;
						}
					}
					
					if (!match) {
						iterator.remove();
						continue;
					}
				}
			}
		}
		
		log.info("Computed list of allowed orgunits to " + allWithSubstitutesAllowed.size() + " based on " +
				 "userConstraintOUUuids.size() = " + userConstraintOUUuids.size() +
				 ", userConstrainedItSystems.size() = " + userConstrainedItSystems.size());

		return allWithSubstitutesAllowed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(List<OrgUnit> orgUnits) {
		orgUnitDao.saveAll(orgUnits);
	}
}
